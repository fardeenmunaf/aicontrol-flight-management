/*
 * flight_plan_validator.c
 *
 * Standalone C validator for the US085 flight plan test/validate feature.
 *
 * Usage: flight_plan_validator <input_json_path>
 *
 * Input JSON schema:
 * {
 *   "flightDesignator": "TP001",
 *   "minimumRequiredArrivalFuel": 2000.0,
 *   "legs": [
 *     {
 *       "legNumber": 1,
 *       "fuelQuantity": 5000.0,
 *       "segments": [
 *         {
 *           "segmentNumber": 1,
 *           "fromAltitude": 10000.0,
 *           "toAltitude": 10000.0,
 *           "altitudeUnit": "ft",
 *           "minimumAltitude": 9000.0,
 *           "maximumAltitude": 12000.0
 *         }
 *       ]
 *     }
 *   ]
 * }
 *
 * Output JSON (to stdout):
 * {
 *   "passed": true,
 *   "failures": []
 * }
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include "libs/cJSON.h"

#define MAX_FAILURES 256
#define MAX_FAILURE_MSG 512

typedef struct {
    char type[64];
    char description[MAX_FAILURE_MSG];
} ValidationFailure;

static ValidationFailure g_failures[MAX_FAILURES];
static int g_failure_count = 0;

static void add_failure(const char *type, const char *fmt, ...) {
    if (g_failure_count >= MAX_FAILURES) return;
    strncpy(g_failures[g_failure_count].type, type, sizeof(g_failures[0].type) - 1);
    va_list args;
    va_start(args, fmt);
    vsnprintf(g_failures[g_failure_count].description,
              MAX_FAILURE_MSG, fmt, args);
    va_end(args);
    g_failure_count++;
}

static char *read_file(const char *path) {
    FILE *f = fopen(path, "rb");
    if (!f) {
        fprintf(stderr, "Cannot open input file: %s\n", path);
        return NULL;
    }
    fseek(f, 0, SEEK_END);
    long size = ftell(f);
    rewind(f);
    char *buf = (char *)malloc(size + 1);
    if (!buf) { fclose(f); return NULL; }
    fread(buf, 1, size, f);
    buf[size] = '\0';
    fclose(f);
    return buf;
}

static void check_fuel(cJSON *legs_array, double min_required_fuel) {
    int num_legs = cJSON_GetArraySize(legs_array);
    if (num_legs == 0) return;

    /* The last leg's fuelQuantity is the fuel available at the start of the final leg.
       This is the minimum fuel reserve that must meet the destination's requirement. */
    cJSON *last_leg = cJSON_GetArrayItem(legs_array, num_legs - 1);
    if (!last_leg) return;

    cJSON *fuel_item = cJSON_GetObjectItem(last_leg, "fuelQuantity");
    if (!fuel_item) {
        add_failure("FUEL_SUFFICIENCY", "Missing fuelQuantity in last leg.");
        return;
    }
    double fuel = fuel_item->valuedouble;

    if (fuel < min_required_fuel) {
        add_failure("FUEL_SUFFICIENCY",
                    "Insufficient fuel: required minimum is %.2f kg at destination, "
                    "current fuel quantity for last leg is %.2f kg.",
                    min_required_fuel, fuel);
    }
}

static void check_altitudes(cJSON *legs_array) {
    int num_legs = cJSON_GetArraySize(legs_array);
    for (int li = 0; li < num_legs; li++) {
        cJSON *leg = cJSON_GetArrayItem(legs_array, li);
        cJSON *leg_num_item = cJSON_GetObjectItem(leg, "legNumber");
        int leg_num = leg_num_item ? leg_num_item->valueint : li + 1;

        cJSON *segments = cJSON_GetObjectItem(leg, "segments");
        if (!segments) continue;

        int num_segs = cJSON_GetArraySize(segments);
        for (int si = 0; si < num_segs; si++) {
            cJSON *seg = cJSON_GetArrayItem(segments, si);
            cJSON *seg_num_item = cJSON_GetObjectItem(seg, "segmentNumber");
            int seg_num = seg_num_item ? seg_num_item->valueint : si + 1;

            cJSON *min_alt_item = cJSON_GetObjectItem(seg, "minimumAltitude");
            cJSON *from_alt_item = cJSON_GetObjectItem(seg, "fromAltitude");
            cJSON *to_alt_item   = cJSON_GetObjectItem(seg, "toAltitude");
            cJSON *unit_item     = cJSON_GetObjectItem(seg, "altitudeUnit");

            if (!min_alt_item || !from_alt_item || !to_alt_item) continue;

            double min_alt  = min_alt_item->valuedouble;
            double from_alt = from_alt_item->valuedouble;
            double to_alt   = to_alt_item->valuedouble;
            const char *unit = (unit_item && unit_item->valuestring) ? unit_item->valuestring : "m";

            if (from_alt < min_alt) {
                add_failure("ALTITUDE_VIOLATION",
                            "Leg %d, Segment %d: planned start altitude %.2f %s is below "
                            "the required minimum of %.2f %s.",
                            leg_num, seg_num, from_alt, unit, min_alt, unit);
            }
            if (to_alt < min_alt) {
                add_failure("ALTITUDE_VIOLATION",
                            "Leg %d, Segment %d: planned end altitude %.2f %s is below "
                            "the required minimum of %.2f %s.",
                            leg_num, seg_num, to_alt, unit, min_alt, unit);
            }
        }
    }
}

static void print_result(void) {
    cJSON *root = cJSON_CreateObject();
    cJSON_AddBoolToObject(root, "passed", g_failure_count == 0 ? 1 : 0);

    cJSON *failures_arr = cJSON_CreateArray();
    for (int i = 0; i < g_failure_count; i++) {
        cJSON *f = cJSON_CreateObject();
        cJSON_AddStringToObject(f, "type", g_failures[i].type);
        cJSON_AddStringToObject(f, "description", g_failures[i].description);
        cJSON_AddItemToArray(failures_arr, f);
    }
    cJSON_AddItemToObject(root, "failures", failures_arr);

    char *out = cJSON_Print(root);
    printf("%s\n", out);
    free(out);
    cJSON_Delete(root);
}

int main(int argc, char *argv[]) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <input_json_path>\n", argv[0]);
        return 1;
    }

    char *json_text = read_file(argv[1]);
    if (!json_text) return 1;

    cJSON *root = cJSON_Parse(json_text);
    free(json_text);

    if (!root) {
        fprintf(stderr, "Failed to parse input JSON: %s\n", cJSON_GetErrorPtr());
        return 1;
    }

    cJSON *min_fuel_item = cJSON_GetObjectItem(root, "minimumRequiredArrivalFuel");
    double min_required_fuel = min_fuel_item ? min_fuel_item->valuedouble : 0.0;

    cJSON *legs = cJSON_GetObjectItem(root, "legs");
    if (legs) {
        check_fuel(legs, min_required_fuel);
        check_altitudes(legs);
    }

    cJSON_Delete(root);
    print_result();
    return 0;
}
