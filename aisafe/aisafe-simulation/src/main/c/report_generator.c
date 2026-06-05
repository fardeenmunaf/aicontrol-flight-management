#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>
#include "structs.h"

#define MAX_FILENAME 256

/**
 * @brief Escapes a string for safe embedding inside a JSON value.
 *
 * @param src     Input string to escape.
 * @param dst     Output buffer.
 * @param dst_len Size of the output buffer in bytes.
 */
static void json_escape_string(const char *src, char *dst, size_t dst_len) {
    size_t dst_pos = 0;
    for (int i = 0; src[i] != '\0' && dst_pos + 1 < dst_len; i++) {
        switch (src[i]) {
            case '"':  dst[dst_pos++] = '\\'; dst[dst_pos++] = '"';  break;
            case '\\': dst[dst_pos++] = '\\'; dst[dst_pos++] = '\\'; break;
            case '\n': dst[dst_pos++] = '\\'; dst[dst_pos++] = 'n';  break;
            case '\r': dst[dst_pos++] = '\\'; dst[dst_pos++] = 'r';  break;
            case '\t': dst[dst_pos++] = '\\'; dst[dst_pos++] = 't';  break;
            default:   dst[dst_pos++] = src[i]; break;
        }
    }
    dst[dst_pos] = '\0';
}

/**
 * @brief Formats a simulation timestamp as an ISO-8601 local datetime string.
 *
 * @param sim_seconds      Simulation clock value in seconds.
 * @param wall_clock_start Wall-clock epoch when the simulation began.
 * @param buf              Output buffer.
 * @param buf_len          Size of output buffer in bytes.
 */
static void format_sim_time(double sim_seconds, double wall_clock_start,
                             char *buf, size_t buf_len) {
    time_t wall_time = (time_t)(wall_clock_start + sim_seconds);
    struct tm *timeinfo = localtime(&wall_time);
    strftime(buf, buf_len, "%Y-%m-%dT%H:%M:%S", timeinfo);
}

/**
 * @brief Writes the full simulation report to a JSON file.
 *
 * @param config      Simulation configuration (for max_violations_threshold).
 * @param report      Simulation report data populated by the engine.
 * @param output_file Path of the JSON file to create.
 */
static void generate_json_report(const SimulationConfig *config,
                                  const SimulationReport *report,
                                  const char *output_file) {
    FILE *fp = fopen(output_file, "w");
    if (!fp) { perror("Failed to open report file"); return; }

    bool sim_passed = (report->total_violations_recorded == 0);

    char report_time_str[32];
    time_t now = time(NULL);
    struct tm *timeinfo = localtime(&now);
    strftime(report_time_str, sizeof(report_time_str), "%Y-%m-%dT%H:%M:%S", timeinfo);

    fprintf(fp, "{\n");
    fprintf(fp, "  \"reportVersion\": \"1.0\",\n");
    fprintf(fp, "  \"reportType\": \"SimulationReport\",\n");
    fprintf(fp, "  \"reportGeneratedAt\": \"%s\",\n\n", report_time_str);

    fprintf(fp, "  \"simulation\": {\n");
    fprintf(fp, "    \"totalFlights\": %d,\n", report->total_flights);
    fprintf(fp, "    \"startTime\": %.1f,\n", report->sim_start_time);
    fprintf(fp, "    \"endTime\": %.1f,\n", report->sim_end_time);
    fprintf(fp, "    \"durationSeconds\": %.1f,\n",
            report->sim_end_time - report->sim_start_time);
    char wall_clock_str[32];
    time_t wall_time = (time_t)report->wall_clock_start;
    timeinfo = localtime(&wall_time);
    strftime(wall_clock_str, sizeof(wall_clock_str), "%Y-%m-%dT%H:%M:%S", timeinfo);
    fprintf(fp, "    \"wallClockStart\": \"%s\"\n", wall_clock_str);
    fprintf(fp, "  },\n\n");

    fprintf(fp, "  \"safetyValidation\": {\n");
    fprintf(fp, "    \"totalViolationsDetected\": %d,\n", report->total_violations_recorded);
    fprintf(fp, "    \"validationResult\": \"%s\",\n", sim_passed ? "PASS" : "FAIL");
    fprintf(fp, "    \"maxViolationsThreshold\": %d\n", config->max_violations_threshold);
    fprintf(fp, "  },\n\n");

    fprintf(fp, "  \"flightExecutionStatuses\": [\n");
    for (int i = 0; i < report->total_flights; i++) {
        const FlightStatus *status = &report->flight_statuses[i];
        char esc[CALLSIGN_LEN * 2];
        json_escape_string(status->callsign, esc, sizeof(esc));
        fprintf(fp, "    {\n");
        fprintf(fp, "      \"flightId\": %d,\n", i);
        fprintf(fp, "      \"callsign\": \"%s\",\n", esc);
        fprintf(fp, "      \"completed\": %s,\n", status->completed_successfully ? "true" : "false");
        fprintf(fp, "      \"hadViolation\": %s,\n", status->had_violation ? "true" : "false");
        fprintf(fp, "      \"fuelRemainingKg\": %.1f,\n", status->fuel_remaining);
        fprintf(fp, "      \"legsCompleted\": %d\n", status->num_legs_completed);
        fprintf(fp, "    }%s\n", (i < report->total_flights - 1) ? "," : "");
    }
    fprintf(fp, "  ],\n\n");

    /* Count actual collision events: entries are zero-initialised, valid ones have timestamp > 0. */
    int num_events = 0;
    for (int i = 0; i < MAX_RECORDED_VIOLATIONS && report->violations[i].timestamp > 0.0; i++)
        num_events++;

    fprintf(fp, "  \"violationEvents\": [\n");
    for (int i = 0; i < num_events; i++) {
        const ViolationEvent *evt = &report->violations[i];
        char time_str[32], esc_a[CALLSIGN_LEN * 2], esc_b[CALLSIGN_LEN * 2];
        format_sim_time(evt->timestamp, report->wall_clock_start, time_str, sizeof(time_str));
        json_escape_string(report->flight_statuses[evt->flight_a_id].callsign, esc_a, sizeof(esc_a));
        json_escape_string(report->flight_statuses[evt->flight_b_id].callsign, esc_b, sizeof(esc_b));

        fprintf(fp, "    {\n");
        fprintf(fp, "      \"violationId\": %d,\n", i);
        fprintf(fp, "      \"timestamp\": %.1f,\n", evt->timestamp);
        fprintf(fp, "      \"wallClockTime\": \"%s\",\n", time_str);
        fprintf(fp, "      \"aircraftA\": {\n");
        fprintf(fp, "        \"flightId\": %d,\n", evt->flight_a_id);
        fprintf(fp, "        \"callsign\": \"%s\",\n", esc_a);
        fprintf(fp, "        \"position\": { \"latitude\": %.6f, \"longitude\": %.6f, \"altitudeMeters\": %.1f },\n",
                evt->pos_a.latitude, evt->pos_a.longitude, evt->pos_a.altitude);
        fprintf(fp, "        \"velocity\": { \"speedMsec\": %.2f, \"headingDegrees\": %.2f, \"verticalSpeedMsec\": %.2f }\n",
                evt->vel_a.speed, evt->vel_a.heading, evt->vel_a.vertical_speed);
        fprintf(fp, "      },\n");
        fprintf(fp, "      \"aircraftB\": {\n");
        fprintf(fp, "        \"flightId\": %d,\n", evt->flight_b_id);
        fprintf(fp, "        \"callsign\": \"%s\",\n", esc_b);
        fprintf(fp, "        \"position\": { \"latitude\": %.6f, \"longitude\": %.6f, \"altitudeMeters\": %.1f },\n",
                evt->pos_b.latitude, evt->pos_b.longitude, evt->pos_b.altitude);
        fprintf(fp, "        \"velocity\": { \"speedMsec\": %.2f, \"headingDegrees\": %.2f, \"verticalSpeedMsec\": %.2f }\n",
                evt->vel_b.speed, evt->vel_b.heading, evt->vel_b.vertical_speed);
        fprintf(fp, "      },\n");
        fprintf(fp, "      \"separationMeters\": %.1f\n", evt->distance);
        fprintf(fp, "    }%s\n", (i < num_events - 1) ? "," : "");
    }
    fprintf(fp, "  ]\n}\n");
    fclose(fp);
    printf("[ReportGen] Report written to: %s\n", output_file);
}

/**
 * @brief Reads SimulationConfig and SimulationReport from a pipe fd passed as argv[1],
 *        then generates a timestamped JSON report file.
 *
 * The pipe fd is inherited from simulation_engine across execl. simulation_engine writes
 * SimulationConfig first, then SimulationReport, then closes its write end.
 *
 * @param argc Must be 2.
 * @param argv argv[1] is the pipe read-end file descriptor number as a string.
 */
int main(int argc, char *argv[]) {
    if (argc != 2) {
        fprintf(stderr, "Usage: report_generator <pipe_fd>\n");
        return EXIT_FAILURE;
    }

    printf("=== AISafe Report Generator ===\n");

    int fd = atoi(argv[1]);

    // Read exactly 'size' bytes, looping over partial reads.
    #define read_all(fd, buf, size) ({ \
        size_t _total = 0; char *_p = (char *)(buf); int _ok = 1; \
        while (_total < (size)) { \
            ssize_t _n = read((fd), _p + _total, (size) - _total); \
            if (_n <= 0) { _ok = 0; break; } _total += (size_t)_n; \
        } _ok; })

    SimulationConfig config;
    SimulationReport report;

    if (!read_all(fd, &config, sizeof(config))) {
        fprintf(stderr, "[ReportGen] Failed to read config from pipe\n");
        close(fd);
        return EXIT_FAILURE;
    }

    if (!read_all(fd, &report, sizeof(report))) {
        fprintf(stderr, "[ReportGen] Failed to read report from pipe\n");
        close(fd);
        return EXIT_FAILURE;
    }

    close(fd);

    time_t now = time(NULL);
    struct tm *timeinfo = localtime(&now);
    char output_file[MAX_FILENAME];
    strftime(output_file, MAX_FILENAME, "simulation_report_%Y%m%d_%H%M%S.json", timeinfo);

    generate_json_report(&config, &report, output_file);

    printf("\n╔════════════════════════════════════════════════════════════════╗\n");
    printf("║                  SIMULATION REPORT SUMMARY                     ║\n");
    printf("╚════════════════════════════════════════════════════════════════╝\n");
    printf("Total Flights:              %d\n", report.total_flights);
    printf("Total Violations Detected:  %d\n", report.total_violations_recorded);
    printf("Simulation Result:          %s\n",
           (report.total_violations_recorded == 0) ? "PASS" : "FAIL");
    printf("Report File:                %s\n\n", output_file);
    printf("%-12s %-12s %-12s\n", "Callsign", "Status", "Violations");
    printf("%-12s %-12s %-12s\n", "--------", "------", "----------");
    for (int i = 0; i < report.total_flights; i++) {
        const FlightStatus *s = &report.flight_statuses[i];
        printf("%-12s %-12s %-12s\n",
               s->callsign,
               s->completed_successfully ? "Completed" : "Incomplete",
               s->had_violation          ? "Yes"       : "No");
    }

    return EXIT_SUCCESS;
}
