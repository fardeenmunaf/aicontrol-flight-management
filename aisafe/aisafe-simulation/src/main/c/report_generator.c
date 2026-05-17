/*
 * report_generator.c — US109 Simulation Report Generator
 *
 * Reads the simulation report from shared memory and generates a comprehensive
 * JSON report file after the simulation completes.
 *
 * Usage:  ./report_generator
 *
 * This program:
 *   1. Opens the shared memory object created by simulation_engine
 *   2. Waits until report_ready flag is set (sim complete)
 *   3. Reads all flight statuses and violation events
 *   4. Generates a JSON report with:
 *      - Total flights and violations
 *      - Per-flight execution status (completed/violations)
 *      - Detailed violation events (timestamps, positions, velocities)
 *      - Pass/fail result
 *   5. Writes report to simulation_report_<timestamp>.json
 *   6. Exits successfully
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <time.h>
#include "structs.h"

#define SHM_NAME "/aisafe_sim"
#define MAX_FILENAME 256

// File permission: read+write for owner, group, and others
#define SHM_PERMISSIONS (S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH)

// Helper to escape JSON strings (basic implementation)
static void json_escape_string(const char *src, char *dst, size_t dst_len) {
    size_t dst_pos = 0;
    for (int i = 0; src[i] != '\0' && dst_pos + 1 < dst_len; i++) {
        switch (src[i]) {
            case '"':  dst[dst_pos++] = '\\'; dst[dst_pos++] = '"'; break;
            case '\\': dst[dst_pos++] = '\\'; dst[dst_pos++] = '\\'; break;
            case '\n': dst[dst_pos++] = '\\'; dst[dst_pos++] = 'n'; break;
            case '\r': dst[dst_pos++] = '\\'; dst[dst_pos++] = 'r'; break;
            case '\t': dst[dst_pos++] = '\\'; dst[dst_pos++] = 't'; break;
            default:   dst[dst_pos++] = src[i]; break;
        }
    }
    dst[dst_pos] = '\0';
}

// Convert simulation timestamp to readable format
static void format_sim_time(double sim_seconds, double wall_clock_start, char *buf, size_t buf_len) {
    time_t wall_time = (time_t)(wall_clock_start + sim_seconds);
    struct tm *timeinfo = localtime(&wall_time);
    strftime(buf, buf_len, "%Y-%m-%dT%H:%M:%S", timeinfo);
}

// Generate the JSON report
static void generate_json_report(SharedMemoryData *shm, const char *output_file) {
    FILE *fp = fopen(output_file, "w");
    if (!fp) {
        perror("Failed to open report file for writing");
        return;
    }

    SimulationReport *report = &shm->report;

    // Determine pass/fail: pass if no violations, fail otherwise
    bool sim_passed = (report->total_violations_recorded == 0);

    fprintf(fp, "{\n");
    fprintf(fp, "  \"reportVersion\": \"1.0\",\n");
    fprintf(fp, "  \"reportType\": \"SimulationReport\",\n");
    fprintf(fp, "  \"reportGeneratedAt\": \"%ld\",\n", (long)time(NULL));
    fprintf(fp, "\n");

    // Simulation overview
    fprintf(fp, "  \"simulation\": {\n");
    fprintf(fp, "    \"totalFlights\": %d,\n", report->total_flights);
    fprintf(fp, "    \"startTime\": %.1f,\n", report->sim_start_time);
    fprintf(fp, "    \"endTime\": %.1f,\n", report->sim_end_time);
    fprintf(fp, "    \"durationSeconds\": %.1f,\n", report->sim_end_time - report->sim_start_time);
    fprintf(fp, "    \"wallClockStart\": \"%ld\"\n", (long)report->wall_clock_start);
    fprintf(fp, "  },\n");
    fprintf(fp, "\n");

    // Safety summary
    fprintf(fp, "  \"safetyValidation\": {\n");
    fprintf(fp, "    \"totalViolationsDetected\": %d,\n", report->total_violations_recorded);
    fprintf(fp, "    \"validationResult\": \"%s\",\n", sim_passed ? "PASS" : "FAIL");
    fprintf(fp, "    \"maxViolationsThreshold\": %d\n", shm->config.max_violations_threshold);
    fprintf(fp, "  },\n");
    fprintf(fp, "\n");

    // Flight statuses
    fprintf(fp, "  \"flightExecutionStatuses\": [\n");
    for (int i = 0; i < report->total_flights; i++) {
        FlightStatus *status = &report->flight_statuses[i];
        char escaped_callsign[CALLSIGN_LEN * 2];
        json_escape_string(status->callsign, escaped_callsign, sizeof(escaped_callsign));

        fprintf(fp, "    {\n");
        fprintf(fp, "      \"flightId\": %d,\n", i);
        fprintf(fp, "      \"callsign\": \"%s\",\n", escaped_callsign);
        fprintf(fp, "      \"completed\": %s,\n", status->completed_successfully ? "true" : "false");
        fprintf(fp, "      \"hadViolation\": %s,\n", status->had_violation ? "true" : "false");
        fprintf(fp, "      \"fuelRemainingKg\": %.1f,\n", status->fuel_remaining);
        fprintf(fp, "      \"legsCompleted\": %d\n", status->num_legs_completed);
        fprintf(fp, "    }%s\n", (i < report->total_flights - 1) ? "," : "");
    }
    fprintf(fp, "  ],\n");
    fprintf(fp, "\n");

    // Violation events
    fprintf(fp, "  \"violationEvents\": [\n");
    for (int i = 0; i < report->total_violations_recorded; i++) {
        ViolationEvent *evt = &report->violations[i];
        char time_str[32];
        char escaped_callsign_a[CALLSIGN_LEN * 2];
        char escaped_callsign_b[CALLSIGN_LEN * 2];

        format_sim_time(evt->timestamp, report->wall_clock_start, time_str, sizeof(time_str));
        json_escape_string(report->flight_statuses[evt->flight_a_id].callsign,
                           escaped_callsign_a, sizeof(escaped_callsign_a));
        json_escape_string(report->flight_statuses[evt->flight_b_id].callsign,
                           escaped_callsign_b, sizeof(escaped_callsign_b));

        fprintf(fp, "    {\n");
        fprintf(fp, "      \"violationId\": %d,\n", i);
        fprintf(fp, "      \"timestamp\": %.1f,\n", evt->timestamp);
        fprintf(fp, "      \"wallClockTime\": \"%s\",\n", time_str);
        fprintf(fp, "      \"aircraftA\": {\n");
        fprintf(fp, "        \"flightId\": %d,\n", evt->flight_a_id);
        fprintf(fp, "        \"callsign\": \"%s\",\n", escaped_callsign_a);
        fprintf(fp, "        \"position\": {\n");
        fprintf(fp, "          \"latitude\": %.6f,\n", evt->pos_a.latitude);
        fprintf(fp, "          \"longitude\": %.6f,\n", evt->pos_a.longitude);
        fprintf(fp, "          \"altitudeMeters\": %.1f\n", evt->pos_a.altitude);
        fprintf(fp, "        },\n");
        fprintf(fp, "        \"velocity\": {\n");
        fprintf(fp, "          \"speedMsec\": %.2f,\n", evt->vel_a.speed);
        fprintf(fp, "          \"headingDegrees\": %.2f,\n", evt->vel_a.heading);
        fprintf(fp, "          \"verticalSpeedMsec\": %.2f\n", evt->vel_a.vertical_speed);
        fprintf(fp, "        }\n");
        fprintf(fp, "      },\n");
        fprintf(fp, "      \"aircraftB\": {\n");
        fprintf(fp, "        \"flightId\": %d,\n", evt->flight_b_id);
        fprintf(fp, "        \"callsign\": \"%s\",\n", escaped_callsign_b);
        fprintf(fp, "        \"position\": {\n");
        fprintf(fp, "          \"latitude\": %.6f,\n", evt->pos_b.latitude);
        fprintf(fp, "          \"longitude\": %.6f,\n", evt->pos_b.longitude);
        fprintf(fp, "          \"altitudeMeters\": %.1f\n", evt->pos_b.altitude);
        fprintf(fp, "        },\n");
        fprintf(fp, "        \"velocity\": {\n");
        fprintf(fp, "          \"speedMsec\": %.2f,\n", evt->vel_b.speed);
        fprintf(fp, "          \"headingDegrees\": %.2f,\n", evt->vel_b.heading);
        fprintf(fp, "          \"verticalSpeedMsec\": %.2f\n", evt->vel_b.vertical_speed);
        fprintf(fp, "        }\n");
        fprintf(fp, "      },\n");
        fprintf(fp, "      \"separationMeters\": %.1f\n", evt->distance);
        fprintf(fp, "    }%s\n", (i < report->total_violations_recorded - 1) ? "," : "");
    }
    fprintf(fp, "  ]\n");

    fprintf(fp, "}\n");
    fclose(fp);

    printf("[ReportGen] Report written to: %s\n", output_file);
}

// Attach to shared memory and generate report when ready
int main(void) {
    printf("=== AISafe Simulation Report Generator (US109) ===\n");

    // Open shared memory, retrying until the simulation creates it.
    const int open_timeout_seconds = 120;
    int fd = -1;
    for (int elapsed = 0; elapsed < open_timeout_seconds; elapsed++) {
        fd = shm_open(SHM_NAME, O_RDONLY, SHM_PERMISSIONS);
        if (fd >= 0) {
            break;
        }
        if (elapsed == 0) {
            perror("shm_open failed");
        }
        sleep(1);
    }
    if (fd < 0) {
        fprintf(stderr, "Failed to open shared memory after waiting %d seconds\n", open_timeout_seconds);
        return EXIT_FAILURE;
    }

    // Map shared memory
    SharedMemoryData *shm = mmap(NULL, sizeof(SharedMemoryData),
                                  PROT_READ, MAP_SHARED, fd, 0);
    close(fd);

    if (shm == MAP_FAILED) {
        perror("mmap failed");
        return EXIT_FAILURE;
    }

    printf("[ReportGen] Connected to shared memory. Waiting for simulation to complete...\n");

    // Wait for simulation to complete (poll report_ready flag)
    int timeout_seconds = 3600; // 1 hour timeout
    int elapsed = 0;
    while (!shm->report.report_ready && elapsed < timeout_seconds) {
        sleep(1);
        elapsed++;
        if (elapsed % 10 == 0) {
            printf("[ReportGen] Still waiting... (%d seconds elapsed)\n", elapsed);
        }
    }

    if (!shm->report.report_ready) {
        fprintf(stderr, "[ReportGen] ERROR: Simulation did not complete within timeout\n");
        munmap(shm, sizeof(SharedMemoryData));
        return EXIT_FAILURE;
    }

    printf("[ReportGen] Simulation complete. Generating report...\n");

    // Generate output filename with timestamp
    time_t now = time(NULL);
    struct tm *timeinfo = localtime(&now);
    char output_file[MAX_FILENAME];
    strftime(output_file, MAX_FILENAME, "simulation_report_%Y%m%d_%H%M%S.json", timeinfo);

    // Generate JSON report
    generate_json_report(shm, output_file);

    // Print summary to console
    printf("\n");
    printf("╔════════════════════════════════════════════════════════════════╗\n");
    printf("║                  SIMULATION REPORT SUMMARY                     ║\n");
    printf("╚════════════════════════════════════════════════════════════════╝\n");
    printf("Total Flights:              %d\n", shm->report.total_flights);
    printf("Total Violations Detected:  %d\n", shm->report.total_violations_recorded);
    printf("Simulation Result:          %s\n",
           (shm->report.total_violations_recorded == 0) ? "PASS" : "FAIL");
    printf("Report File:                %s\n", output_file);
    printf("\nFlight Status Summary:\n");
    printf("%-12s %-12s %-12s\n", "Callsign", "Status", "Violations");
    printf("%-12s %-12s %-12s\n", "--------", "------", "----------");
    for (int i = 0; i < shm->report.total_flights; i++) {
        FlightStatus *status = &shm->report.flight_statuses[i];
        const char *status_str = status->completed_successfully ? "Completed" : "Incomplete";
        const char *viol_str = status->had_violation ? "Yes" : "No";
        printf("%-12s %-12s %-12s\n", status->callsign, status_str, viol_str);
    }

    // Cleanup
    munmap(shm, sizeof(SharedMemoryData));
    shm_unlink(SHM_NAME);

    printf("\nReport generation complete.\n");
    return EXIT_SUCCESS;
}

