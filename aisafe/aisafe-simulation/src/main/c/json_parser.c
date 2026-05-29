#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <dirent.h>
#include "cJSON.h"
#include "json_parser.h"

// ── Validation helpers ─────────────────────────────────────────────────────────

/**
 * @brief Checks that a required numeric field is present and positive (> 0).
 *
 * Used for fields that are denominators in physics formulas — a zero or missing
 * value would cause division by zero or physically nonsensical results.
 *
 * @param field    Human-readable field name for the error message.
 * @param label    Callsign or file label for the error message.
 * @param item     cJSON item to check (may be NULL if field was missing).
 * @param out      Pointer to the double to assign if valid.
 * @return 1 if valid and assigned, 0 otherwise.
 */
static int require_positive(const char *field, const char *label,
                             cJSON *item, double *out) {
    if (!cJSON_IsNumber(item)) {
        fprintf(stderr, "[%s] Missing required field '%s'\n", label, field);
        return 0;
    }
    if (item->valuedouble <= 0.0) {
        fprintf(stderr, "[%s] '%s' must be > 0 (got %.4g)\n", label, field, item->valuedouble);
        return 0;
    }
    *out = item->valuedouble;
    return 1;
}

/**
 * @brief Checks that a required numeric field is present and within [min, max].
 *
 * @param field    Human-readable field name for the error message.
 * @param label    Callsign or file label for the error message.
 * @param item     cJSON item to check (may be NULL if field was missing).
 * @param min      Inclusive lower bound.
 * @param max      Inclusive upper bound.
 * @param out      Pointer to the double to assign if valid.
 * @return 1 if valid and assigned, 0 otherwise.
 */
static int require_range(const char *field, const char *label,
                          cJSON *item, double min, double max, double *out) {
    if (!cJSON_IsNumber(item)) {
        fprintf(stderr, "[%s] Missing required field '%s'\n", label, field);
        return 0;
    }
    if (item->valuedouble < min || item->valuedouble > max) {
        fprintf(stderr, "[%s] '%s' out of range: %.4g not in [%.4g, %.4g]\n",
                label, field, item->valuedouble, min, max);
        return 0;
    }
    *out = item->valuedouble;
    return 1;
}

/**
 * @brief Reads an entire file into a heap-allocated null-terminated string.
 *
 * @param filename Path to the file to read.
 * @return Pointer to the allocated buffer, or NULL if the file cannot be opened.
 *         Caller is responsible for calling free() on the returned pointer.
 */
static char* read_file(const char* filename) {
    FILE *f = fopen(filename, "rb");
    if (!f) return NULL;

    fseek(f, 0, SEEK_END);
    long length = ftell(f);
    fseek(f, 0, SEEK_SET);

    char *data = malloc(length + 1);
    if (data) {
        fread(data, 1, length, f);
        data[length] = '\0';
    }
    fclose(f);
    return data;
}

/**
 * @brief Parses simulation-wide settings from a JSON file into a SimulationConfig.
 *
 * Reads start_time, end_time, time_step, safety_distance_threshold,
 * max_violations_threshold, and sim_start (ISO-8601 datetime).
 * Must be called before any flight plans are loaded.
 *
 * @param filename Path to the simulation settings JSON file.
 * @param config   Output struct to populate.
 * @return 1 on success, 0 if the file cannot be read, parsed, or contains invalid values.
 */
int load_simulation_settings(const char* filename, SimulationConfig* config) {
    char* data = read_file(filename);
    if (!data) {
        fprintf(stderr, "Could not open %s\n", filename);
        return 0;
    }

    cJSON *json = cJSON_Parse(data);
    if (!json) {
        fprintf(stderr, "Error parsing JSON in %s\n", filename);
        free(data);
        return 0;
    }

    cJSON *item;

    item = cJSON_GetObjectItemCaseSensitive(json, "start_time");
    if (cJSON_IsNumber(item)) config->start_time = item->valuedouble;

    item = cJSON_GetObjectItemCaseSensitive(json, "end_time");
    if (cJSON_IsNumber(item)) config->end_time = item->valuedouble;

    item = cJSON_GetObjectItemCaseSensitive(json, "time_step");
    if (cJSON_IsNumber(item)) config->time_step = item->valuedouble;

    item = cJSON_GetObjectItemCaseSensitive(json, "safety_distance_threshold");
    if (cJSON_IsNumber(item)) config->safety_distance_threshold = item->valuedouble;

    item = cJSON_GetObjectItemCaseSensitive(json, "max_violations_threshold");
    if (cJSON_IsNumber(item)) config->max_violations_threshold = item->valueint;

    item = cJSON_GetObjectItemCaseSensitive(json, "sim_start");
    if (cJSON_IsString(item)) {
        const char *t = strchr(item->valuestring, 'T');
        if (t) {
            int h = 0, m = 0, s = 0;
            sscanf(t + 1, "%d:%d:%d", &h, &m, &s);
            config->sim_start_seconds = h * 3600.0 + m * 60.0 + s;
        }
    }

    cJSON_Delete(json);
    free(data);

    if (config->end_time <= config->start_time) {
        fprintf(stderr, "[settings] end_time (%.1f) must be greater than start_time (%.1f)\n",
                config->end_time, config->start_time);
        return 0;
    }

    printf("Loaded settings: start=%.1f end=%.1f step=%.1f dist=%.1f max_viol=%d sim_start=%.0fs (%02.0f:%02.0f)\n",
           config->start_time, config->end_time, config->time_step,
           config->safety_distance_threshold, config->max_violations_threshold,
           config->sim_start_seconds,
           floor(config->sim_start_seconds / 3600.0),
           fmod(config->sim_start_seconds, 3600.0) / 60.0);
    return 1;
}

/**
 * @brief Parses a single flight plan JSON file and appends the flight to shared memory.
 *
 * Supports both wrapped ([ { ... } ]) and direct ({ ... }) JSON formats.
 * Validates all physical values; logs errors and skips the flight plan if any
 * critical field is missing, zero, or physically impossible.
 * Increments shm->num_flights on success.
 *
 * @param filepath Path to the flight plan JSON file.
 * @param shm      Pointer to shared memory; the new flight is written into
 *                 shm->flights[shm->num_flights].
 */
static int parse_flight_plan(const char* filepath, SharedMemoryData* shm) {
    char* data = read_file(filepath);
    if (!data) return 0;

    cJSON *json = cJSON_Parse(data);
    if (!json) {
        fprintf(stderr, "Error parsing flight plan JSON in %s\n", filepath);
        free(data);
        return 0;
    }

    // Handle both [ { ... } ] and { ... } formats.
    cJSON *fp = cJSON_IsArray(json) ? cJSON_GetArrayItem(json, 0) : json;
    if (!fp) {
        cJSON_Delete(json);
        free(data);
        return 0;
    }

    if (shm->num_flights >= MAX_FLIGHTS) {
        fprintf(stderr, "Max flights reached. Skipping %s\n", filepath);
        cJSON_Delete(json);
        free(data);
        return 0;
    }

    Flight *flight = &shm->flights[shm->num_flights];
    flight->id = shm->num_flights + 1;
    flight->is_active = true;
    flight->violation_detected = false;

    const char *label = filepath;

    cJSON *designator = cJSON_GetObjectItemCaseSensitive(fp, "flightDesignator");
    if (cJSON_IsString(designator)) {
        strncpy(flight->callsign, designator->valuestring, CALLSIGN_LEN - 1);
        flight->callsign[CALLSIGN_LEN - 1] = '\0';
        label = flight->callsign;
    }

    int valid = 1;

    // ── Aircraft properties ────────────────────────────────────────────────────
    cJSON *aircraft = cJSON_GetObjectItemCaseSensitive(fp, "aircraft");
    if (cJSON_IsObject(aircraft)) {
        cJSON *item;

        item = cJSON_GetObjectItemCaseSensitive(aircraft, "emptyWeight");
        valid &= require_positive("emptyWeight", label, item, &flight->properties.empty_weight);

        // wing_area and aspect_ratio are denominators in the lift/drag formulas
        item = cJSON_GetObjectItemCaseSensitive(aircraft, "wingArea");
        valid &= require_positive("wingArea", label, item, &flight->properties.wing_area);

        item = cJSON_GetObjectItemCaseSensitive(aircraft, "aspectRatio");
        valid &= require_positive("aspectRatio", label, item, &flight->properties.aspect_ratio);

        // oswald_efficiency is a denominator in the drag formula — must be > 0
        item = cJSON_GetObjectItemCaseSensitive(aircraft, "oswaldEfficiency");
        valid &= require_positive("oswaldEfficiency", label, item, &flight->properties.oswald_efficiency);

        item = cJSON_GetObjectItemCaseSensitive(aircraft, "cd0");
        valid &= require_positive("cd0", label, item, &flight->properties.cd0);

        item = cJSON_GetObjectItemCaseSensitive(aircraft, "numberOfEngines");
        if (!cJSON_IsNumber(item) || item->valueint < 1) {
            fprintf(stderr, "[%s] 'numberOfEngines' must be a positive integer\n", label);
            valid = 0;
        } else {
            flight->properties.num_engines = item->valueint;
        }
    } else {
        fprintf(stderr, "[%s] Missing required 'aircraft' object\n", label);
        valid = 0;
    }

    // ── Engine properties ──────────────────────────────────────────────────────
    cJSON *engine = cJSON_GetObjectItemCaseSensitive(fp, "engine");
    if (cJSON_IsObject(engine)) {
        cJSON *item;

        item = cJSON_GetObjectItemCaseSensitive(engine, "thrustSeaLevelStopped");
        valid &= require_positive("thrustSeaLevelStopped", label, item,
                                  &flight->properties.thrust_sea_level_stopped);

        item = cJSON_GetObjectItemCaseSensitive(engine, "thrustSeaLevelCruise");
        valid &= require_positive("thrustSeaLevelCruise", label, item,
                                  &flight->properties.thrust_sea_level_cruise);

        item = cJSON_GetObjectItemCaseSensitive(engine, "thrustLapseRate");
        valid &= require_positive("thrustLapseRate", label, item,
                                  &flight->properties.thrust_lapse_rate);

        item = cJSON_GetObjectItemCaseSensitive(engine, "thrustSpecificFuelConsumption");
        valid &= require_positive("thrustSpecificFuelConsumption", label, item,
                                  &flight->properties.tsfc);

        // Cruise thrust must be less than static thrust — otherwise the lambda slope
        // in the thrust formula goes negative and thrust grows with speed.
        if (valid &&
            flight->properties.thrust_sea_level_cruise >= flight->properties.thrust_sea_level_stopped) {
            fprintf(stderr, "[%s] thrustSeaLevelCruise (%.0f N) must be less than "
                    "thrustSeaLevelStopped (%.0f N)\n",
                    label,
                    flight->properties.thrust_sea_level_cruise,
                    flight->properties.thrust_sea_level_stopped);
            valid = 0;
        }
    } else {
        fprintf(stderr, "[%s] Missing required 'engine' object\n", label);
        valid = 0;
    }

    if (!valid) {
        fprintf(stderr, "[%s] Flight plan rejected due to invalid aircraft/engine values\n", label);
        cJSON_Delete(json);
        free(data);
        return 0;
    }

    // ── Legs → waypoints + mass budget ────────────────────────────────────────
    cJSON *legs = cJSON_GetObjectItemCaseSensitive(fp, "legs");
    int wp_index = 0;
    int leg_index = 0;

    if (cJSON_IsArray(legs)) {
        cJSON *leg;
        cJSON_ArrayForEach(leg, legs) {
            if (leg_index >= MAX_LEGS) {
                fprintf(stderr, "[%s] Too many legs (max %d). Skipping remaining.\n",
                        label, MAX_LEGS);
                break;
            }

            Leg *cur_leg = &flight->legs[leg_index];
            cur_leg->scheduled_arrival_offset_s = ARRIVAL_OFFSET_NOT_SET;

            cJSON *fuel_obj = cJSON_GetObjectItemCaseSensitive(leg, "fuel");
            if (cJSON_IsObject(fuel_obj)) {
                cJSON *qty  = cJSON_GetObjectItemCaseSensitive(fuel_obj, "quantity");
                cJSON *unit = cJSON_GetObjectItemCaseSensitive(fuel_obj, "unit");
                if (cJSON_IsNumber(qty)) {
                    double fuel_kg = cJSON_IsString(unit) && strcmp(unit->valuestring, "kg") == 0
                                     ? qty->valuedouble
                                     : qty->valuedouble * FUEL_DENSITY_KG_PER_L;
                    if (fuel_kg <= 0.0) {
                        fprintf(stderr, "[%s] Leg %d: fuel must be > 0 (got %.4g kg)\n",
                                label, leg_index, fuel_kg);
                        valid = 0;
                    } else {
                        cur_leg->fuel_mass = fuel_kg;
                    }
                }
            }

            cJSON *payload_obj = cJSON_GetObjectItemCaseSensitive(leg, "payload");
            if (cJSON_IsObject(payload_obj)) {
                cJSON *qty = cJSON_GetObjectItemCaseSensitive(payload_obj, "quantity");
                if (cJSON_IsNumber(qty)) {
                    if (qty->valuedouble < 0.0) {
                        fprintf(stderr, "[%s] Leg %d: payload must be >= 0 (got %.4g kg)\n",
                                label, leg_index, qty->valuedouble);
                        valid = 0;
                    } else {
                        cur_leg->payload = qty->valuedouble;
                    }
                }
            }

            double departure_s = -1.0;
            cJSON *dep_obj = cJSON_GetObjectItemCaseSensitive(leg, "departure");
            if (cJSON_IsObject(dep_obj)) {
                cJSON *dep_time = cJSON_GetObjectItemCaseSensitive(dep_obj, "time");
                if (cJSON_IsString(dep_time)) {
                    int dh = 0, dm = 0;
                    if (sscanf(dep_time->valuestring, "%d:%d", &dh, &dm) == 2) {
                        departure_s = dh * 3600.0 + dm * 60.0;
                        cur_leg->departure_offset_s = departure_s - shm->config.sim_start_seconds;
                    }
                }
            }

            cJSON *arr_obj = cJSON_GetObjectItemCaseSensitive(leg, "arrival");
            if (cJSON_IsObject(arr_obj)) {
                cJSON *arr_time = cJSON_GetObjectItemCaseSensitive(arr_obj, "time");
                if (cJSON_IsString(arr_time)) {
                    int ah = 0, am = 0;
                    if (sscanf(arr_time->valuestring, "%d:%d", &ah, &am) == 2) {
                        double arrive_s = ah * 3600.0 + am * 60.0;
                        if (departure_s >= 0.0 && arrive_s <= departure_s) {
                            fprintf(stderr, "[%s] Leg %d: arrival time must be after departure time\n",
                                    label, leg_index);
                            valid = 0;
                        } else {
                            cur_leg->scheduled_arrival_offset_s = arrive_s - shm->config.sim_start_seconds;
                        }
                    }
                }
            }

            // waypoint_start_index for leg 0 is 1: skip the ground departure waypoint at index 0.
            int leg_wp_start_before = wp_index;

            cJSON *segments = cJSON_GetObjectItemCaseSensitive(leg, "segments");
            if (cJSON_IsArray(segments)) {
                cJSON *segment;
                cJSON_ArrayForEach(segment, segments) {
                    if (wp_index >= MAX_WAYPOINTS) break;

                    FlightPhase seg_phase = PHASE_CLIMB;
                    cJSON *mode_json = cJSON_GetObjectItemCaseSensitive(segment, "mode");
                    if (cJSON_IsString(mode_json)) {
                        if (strcmp(mode_json->valuestring, "cruise") == 0)
                            seg_phase = PHASE_CRUISE;
                        else if (strcmp(mode_json->valuestring, "descend") == 0)
                            seg_phase = PHASE_DESCEND;
                    }

                    // Add the "from" position only for the very first segment: seeds waypoint[0].
                    if (wp_index == 0) {
                        cJSON *from = cJSON_GetObjectItemCaseSensitive(segment, "from");
                        if (cJSON_IsObject(from)) {
                            double lat, lon, alt;
                            valid &= require_range("from.latitude",  label,
                                cJSON_GetObjectItemCaseSensitive(from, "latitude"),  -90.0,   90.0, &lat);
                            valid &= require_range("from.longitude", label,
                                cJSON_GetObjectItemCaseSensitive(from, "longitude"), -180.0, 180.0, &lon);
                            valid &= require_range("from.altitude",  label,
                                cJSON_GetObjectItemCaseSensitive(from, "altitude"),  -500.0, 20000.0, &alt);
                            flight->flight_plan.waypoints[wp_index].pos.latitude  = lat;
                            flight->flight_plan.waypoints[wp_index].pos.longitude = lon;
                            flight->flight_plan.waypoints[wp_index].pos.altitude  = alt;
                            flight->current_pos.latitude  = lat;
                            flight->current_pos.longitude = lon;
                            flight->current_pos.altitude  = alt;
                            flight->flight_plan.waypoints[wp_index].target_speed = 250.0;
                            flight->flight_plan.waypoints[wp_index].segment_phase = PHASE_CLIMB;
                            wp_index++;
                        }
                    }

                    if (wp_index >= MAX_WAYPOINTS) break;

                    cJSON *to = cJSON_GetObjectItemCaseSensitive(segment, "to");
                    if (cJSON_IsObject(to)) {
                        double lat, lon, alt;
                        valid &= require_range("to.latitude",  label,
                            cJSON_GetObjectItemCaseSensitive(to, "latitude"),  -90.0,   90.0, &lat);
                        valid &= require_range("to.longitude", label,
                            cJSON_GetObjectItemCaseSensitive(to, "longitude"), -180.0, 180.0, &lon);
                        valid &= require_range("to.altitude",  label,
                            cJSON_GetObjectItemCaseSensitive(to, "altitude"),  -500.0, 20000.0, &alt);
                        flight->flight_plan.waypoints[wp_index].pos.latitude  = lat;
                        flight->flight_plan.waypoints[wp_index].pos.longitude = lon;
                        flight->flight_plan.waypoints[wp_index].pos.altitude  = alt;
                        flight->flight_plan.waypoints[wp_index].target_speed  = 250.0;
                        flight->flight_plan.waypoints[wp_index].segment_phase = seg_phase;
                        wp_index++;
                    }
                }
            }

            cur_leg->waypoint_start_index = (leg_index == 0) ? 1 : leg_wp_start_before;
            cur_leg->waypoint_end_index   = wp_index - 1;

            leg_index++;
        }
    }

    if (!valid) {
        fprintf(stderr, "[%s] Flight plan rejected due to invalid leg/waypoint values\n", label);
        cJSON_Delete(json);
        free(data);
        return 0;
    }

    flight->num_legs    = leg_index;
    flight->current_leg = 0;
    flight->leg_just_completed = false;

    flight->payload            = flight->legs[0].payload;
    flight->initial_fuel_mass  = flight->legs[0].fuel_mass;
    flight->initial_mass       = flight->properties.empty_weight + flight->payload + flight->initial_fuel_mass;
    flight->current_mass       = flight->initial_mass;
    flight->departure_offset_s = flight->legs[0].departure_offset_s;

    flight->flight_plan.num_waypoints          = wp_index;
    flight->flight_plan.current_waypoint_index = 1;

    printf("Loaded flight plan for %s: departs at offset %.0fs (%.0f min). "
           "empty=%.0f payload=%.0f fuel=%.0f initial_mass=%.0f kg\n",
           flight->callsign,
           flight->departure_offset_s,
           flight->departure_offset_s / 60.0,
           flight->properties.empty_weight, flight->payload,
           flight->initial_fuel_mass, flight->initial_mass);

    shm->num_flights++;

    cJSON_Delete(json);
    free(data);
    return 1;
}

/**
 * @brief Scans a directory for .json files and loads each as a flight plan.
 *
 * Calls parse_flight_plan() for every file whose name ends in ".json".
 * Directory scan order depends on the filesystem (not necessarily alphabetical).
 * If any flight plan fails validation, all previously loaded flights are still
 * present in shm but the return value signals failure so the caller can abort.
 *
 * @param directory_path Path to the directory containing flight plan JSON files.
 * @param shm            Pointer to shared memory; flights are appended to shm->flights[].
 * @return 1 if all flight plans loaded successfully, 0 if any failed validation.
 */
int load_all_flight_plans(const char* directory_path, SharedMemoryData* shm) {
    DIR *dir;
    struct dirent *ent;
    int all_valid = 1;

    if ((dir = opendir(directory_path)) != NULL) {
        while ((ent = readdir(dir)) != NULL) {
            size_t len = strlen(ent->d_name);
            if (len > 5 && strcmp(ent->d_name + len - 5, ".json") == 0) {
                char filepath[1024];
                snprintf(filepath, sizeof(filepath), "%s/%s", directory_path, ent->d_name);
                if (!parse_flight_plan(filepath, shm))
                    all_valid = 0;
            }
        }
        closedir(dir);
    } else {
        perror("Could not open flightplansSim directory");
        return 0;
    }

    return all_valid;
}
