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

#define SHM_PERMISSIONS (S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH)

/**
 * @brief Escapes a string for safe embedding inside a JSON value.
 *
 * Replaces ", \, newline, carriage return, and tab with their JSON escape sequences.
 * Output is always null-terminated and will not exceed dst_len bytes.
 *
 * @param src     Input string to escape.
 * @param dst     Output buffer to write the escaped string into.
 * @param dst_len Size of the output buffer in bytes.
 */
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

/**
 * @brief Formats a simulation timestamp as an ISO-8601 local datetime string.
 *
 * Converts sim_seconds (offset from sim start) to a wall-clock time by adding
 * wall_clock_start, then formats the result with strftime.
 *
 * @param sim_seconds      Simulation clock value in seconds.
 * @param wall_clock_start Wall-clock epoch (seconds) when the simulation began.
 * @param buf              Output buffer to write the formatted string into.
 * @param buf_len          Size of the output buffer in bytes.
 */
static void format_sim_time(double sim_seconds, double wall_clock_start, char *buf, size_t buf_len) {
    time_t wall_time = (time_t)(wall_clock_start + sim_seconds);
    struct tm *timeinfo = localtime(&wall_time);
    strftime(buf, buf_len, "%Y-%m-%dT%H:%M:%S", timeinfo);
}

/**
 * @brief Reads the simulation report from shared memory and writes a JSON report file.
 *
 * Outputs report metadata, a simulation overview, a safety validation summary,
 * per-flight execution statuses, and detailed violation events with positions
 * and velocities for both involved aircraft.
 *
 * @param shm         Pointer to the shared memory block (read-only).
 * @param output_file Path of the JSON file to create.
 */
static void generate_json_report(SharedMemoryData *shm, const char *output_file) {
    FILE *fp = fopen(output_file, "w");
    if (!fp) {
        perror("Failed to open report file for writing");
        return;
    }

    SimulationReport *report = &shm->report;
    bool sim_passed = (report->total_violations_recorded == 0);

    fprintf(fp, "{\n");
    fprintf(fp, "  \"reportVersion\": \"1.0\",\n");
    fprintf(fp, "  \"reportType\": \"SimulationReport\",\n");

    char report_time_str[32];
    time_t now = time(NULL);
    struct tm *timeinfo = localtime(&now);
    strftime(report_time_str, sizeof(report_time_str), "%Y-%m-%dT%H:%M:%S", timeinfo);
    fprintf(fp, "  \"reportGeneratedAt\": \"%s\",\n", report_time_str);
    fprintf(fp, "\n");

    fprintf(fp, "  \"simulation\": {\n");
    fprintf(fp, "    \"totalFlights\": %d,\n", report->total_flights);
    fprintf(fp, "    \"startTime\": %.1f,\n", report->sim_start_time);
    fprintf(fp, "    \"endTime\": %.1f,\n", report->sim_end_time);
    fprintf(fp, "    \"durationSeconds\": %.1f,\n", report->sim_end_time - report->sim_start_time);

    char wall_clock_str[32];
    time_t wall_time = (time_t)report->wall_clock_start;
    timeinfo = localtime(&wall_time);
    strftime(wall_clock_str, sizeof(wall_clock_str), "%Y-%m-%dT%H:%M:%S", timeinfo);
    fprintf(fp, "    \"wallClockStart\": \"%s\"\n", wall_clock_str);
    fprintf(fp, "  },\n");
    fprintf(fp, "\n");

    fprintf(fp, "  \"safetyValidation\": {\n");
    fprintf(fp, "    \"totalViolationsDetected\": %d,\n", report->total_violations_recorded);
    fprintf(fp, "    \"validationResult\": \"%s\",\n", sim_passed ? "PASS" : "FAIL");
    fprintf(fp, "    \"maxViolationsThreshold\": %d\n", shm->config.max_violations_threshold);
    fprintf(fp, "  },\n");
    fprintf(fp, "\n");

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

int main(void) {
    printf("=== AISafe Simulation Report Generator (US109) ===\n");

    const int open_timeout_seconds = 120;
    int fd = -1;
    for (int elapsed = 0; elapsed < open_timeout_seconds; elapsed++) {
        fd = shm_open(SHM_NAME, O_RDONLY, SHM_PERMISSIONS);
        if (fd >= 0) break;
        if (elapsed == 0) perror("shm_open failed");
        sleep(1);
    }
    if (fd < 0) {
        fprintf(stderr, "Failed to open shared memory after waiting %d seconds\n", open_timeout_seconds);
        return EXIT_FAILURE;
    }

    SharedMemoryData *shm = mmap(NULL, sizeof(SharedMemoryData),
                                  PROT_READ, MAP_SHARED, fd, 0);
    close(fd);

    if (shm == MAP_FAILED) {
        perror("mmap failed");
        return EXIT_FAILURE;
    }

    printf("[ReportGen] Connected to shared memory. Waiting for simulation to complete...\n");

    int timeout_seconds = 3600;
    int elapsed = 0;
    while (!shm->report.report_ready && elapsed < timeout_seconds) {
        sleep(1);
        elapsed++;
        if (elapsed % 10 == 0)
            printf("[ReportGen] Still waiting... (%d seconds elapsed)\n", elapsed);
    }

    if (!shm->report.report_ready) {
        fprintf(stderr, "[ReportGen] ERROR: Simulation did not complete within timeout\n");
        munmap(shm, sizeof(SharedMemoryData));
        return EXIT_FAILURE;
    }

    printf("[ReportGen] Simulation complete. Generating report...\n");

    time_t now = time(NULL);
    struct tm *timeinfo = localtime(&now);
    char output_file[MAX_FILENAME];
    strftime(output_file, MAX_FILENAME, "simulation_report_%Y%m%d_%H%M%S.json", timeinfo);

    generate_json_report(shm, output_file);

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

    munmap(shm, sizeof(SharedMemoryData));
    shm_unlink(SHM_NAME);

    printf("\nReport generation complete.\n");
    return EXIT_SUCCESS;
}
