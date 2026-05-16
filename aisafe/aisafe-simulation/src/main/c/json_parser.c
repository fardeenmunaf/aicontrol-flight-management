#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>       // floor, fmod — for time formatting
#include <dirent.h>     // opendir, readdir, closedir — to scan a directory
#include "cJSON.h"      // third-party JSON parser library
#include "json_parser.h"

// ── Helper: load an entire file into a heap-allocated string ──────────────────

// Returns a null-terminated string with the full file contents.
// Caller is responsible for calling free() on the returned pointer.

static char* read_file(const char* filename) {
    FILE *f = fopen(filename, "rb"); // "rb" = read binary, exact byte counts
    if (!f) return NULL;

    fseek(f, 0, SEEK_END);           // jump to end of file
    long length = ftell(f);          // ftell() returns current position = file size
    fseek(f, 0, SEEK_SET);           // rewind to the beginning

    char *data = malloc(length + 1); // +1 for the null terminator we add below
    if (data) {
        fread(data, 1, length, f);   // read all bytes into the buffer
        data[length] = '\0';         // null-terminate so C string functions work
    }
    fclose(f);
    return data;
}

// ── Parse simulation_settings.json ────────────────────────────────────────────

// Reads simulation-wide parameters (time range, step size, violation threshold).
// Must be called before any flight plans are loaded.

void load_simulation_settings(const char* filename, SimulationConfig* config) {
    char* data = read_file(filename);
    if (!data) {
        fprintf(stderr, "Could not open %s\n", filename);
        return;
    }

    // cJSON_Parse() converts the raw JSON string into a tree of cJSON nodes
    cJSON *json = cJSON_Parse(data);
    if (!json) {
        fprintf(stderr, "Error parsing JSON in %s\n", filename);
        free(data);
        return;
    }

    // cJSON_GetObjectItemCaseSensitive() looks up a key in the JSON object.
    // cJSON_IsNumber() checks that the value is actually a number before reading it.
    // This avoids crashes if a key is missing or has the wrong type.

    cJSON *item = cJSON_GetObjectItemCaseSensitive(json, "start_time");
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
        // "2026-05-20T06:00:00" — extract HH:MM:SS from after the 'T'
        const char *t = strchr(item->valuestring, 'T');
        if (t) {
            int h = 0, m = 0, s = 0;
            sscanf(t + 1, "%d:%d:%d", &h, &m, &s);
            config->sim_start_seconds = h * 3600.0 + m * 60.0 + s;
        }
    }

    cJSON_Delete(json); // free the entire cJSON tree (all nodes recursively)
    free(data);         // free the raw string buffer we allocated in read_file()
    printf("Loaded settings: start=%.1f end=%.1f step=%.1f dist=%.1f max_viol=%d sim_start=%.0fs (%02.0f:%02.0f)\n",
           config->start_time, config->end_time, config->time_step,
           config->safety_distance_threshold, config->max_violations_threshold,
           config->sim_start_seconds,
           floor(config->sim_start_seconds / 3600.0),
           fmod(config->sim_start_seconds, 3600.0) / 60.0);
}

// ── Parse a single flight plan JSON file ──────────────────────────────────────

// Fills the next free slot in shm->flights[] with all data for one flight.
// The JSON structure is: { flightDesignator, aircraft{}, engine{}, legs:[{...}] }

static void parse_flight_plan(const char* filepath, SharedMemoryData* shm) {
    char* data = read_file(filepath);
    if (!data) return;

    cJSON *json = cJSON_Parse(data);
    if (!json) {
        fprintf(stderr, "Error parsing flight plan JSON in %s\n", filepath);
        free(data);
        return;
    }

    // Some JSON files wrap the plan in an array — handle both formats:
    //   [ { ... } ]   →  take the first array element
    //   { ... }       →  use it directly

    cJSON *fp = cJSON_IsArray(json) ? cJSON_GetArrayItem(json, 0) : json;
    if (!fp) {
        cJSON_Delete(json);
        free(data);
        return;
    }

    // Guard against loading more flights than the shared memory can hold

    if (shm->num_flights >= MAX_FLIGHTS) {
        fprintf(stderr, "Max flights reached. Skipping %s\n", filepath);
        cJSON_Delete(json);
        free(data);
        return;
    }

    // Get a pointer to the next free Flight slot in the shared memory array

    Flight *flight = &shm->flights[shm->num_flights];
    flight->id = shm->num_flights + 1; // 1-based ID
    flight->is_active = true;           // starts as active; set to false when it lands
    flight->violation_detected = false;

    // ── callsign ──────────────────────────────────────────────────────────────

    cJSON *designator = cJSON_GetObjectItemCaseSensitive(fp, "flightDesignator");
    if (cJSON_IsString(designator)) {
        // strncpy with explicit null-termination prevents buffer overflows
        strncpy(flight->callsign, designator->valuestring, CALLSIGN_LEN - 1);
        flight->callsign[CALLSIGN_LEN - 1] = '\0';
    }

    // ── aircraft physical properties ──────────────────────────────────────────

    // These are the aerodynamic constants used by every physics formula.

    cJSON *aircraft = cJSON_GetObjectItemCaseSensitive(fp, "aircraft");
    if (cJSON_IsObject(aircraft)) {
        cJSON *item;
        item = cJSON_GetObjectItemCaseSensitive(aircraft, "emptyWeight");
        if (cJSON_IsNumber(item)) flight->properties.empty_weight = item->valuedouble;

        item = cJSON_GetObjectItemCaseSensitive(aircraft, "wingArea");
        if (cJSON_IsNumber(item)) flight->properties.wing_area = item->valuedouble;

        item = cJSON_GetObjectItemCaseSensitive(aircraft, "aspectRatio");
        if (cJSON_IsNumber(item)) flight->properties.aspect_ratio = item->valuedouble;

        item = cJSON_GetObjectItemCaseSensitive(aircraft, "oswaldEfficiency");
        if (cJSON_IsNumber(item)) flight->properties.oswald_efficiency = item->valuedouble;

        item = cJSON_GetObjectItemCaseSensitive(aircraft, "cd0");
        if (cJSON_IsNumber(item)) flight->properties.cd0 = item->valuedouble;

        item = cJSON_GetObjectItemCaseSensitive(aircraft, "numberOfEngines");
        if (cJSON_IsNumber(item)) flight->properties.num_engines = item->valueint;
    }

    // ── engine properties ─────────────────────────────────────────────────────

    // Thrust values at two reference points let us build the full thrust curve.

    cJSON *engine = cJSON_GetObjectItemCaseSensitive(fp, "engine");
    if (cJSON_IsObject(engine)) {
        cJSON *item;
        item = cJSON_GetObjectItemCaseSensitive(engine, "thrustSeaLevelStopped");
        if (cJSON_IsNumber(item)) flight->properties.thrust_sea_level_stopped = item->valuedouble;

        item = cJSON_GetObjectItemCaseSensitive(engine, "thrustSeaLevelCruise");
        if (cJSON_IsNumber(item)) flight->properties.thrust_sea_level_cruise = item->valuedouble;

        item = cJSON_GetObjectItemCaseSensitive(engine, "thrustLapseRate");
        if (cJSON_IsNumber(item)) flight->properties.thrust_lapse_rate = item->valuedouble;

        item = cJSON_GetObjectItemCaseSensitive(engine, "thrustSpecificFuelConsumption");
        if (cJSON_IsNumber(item)) flight->properties.tsfc = item->valuedouble;
    }

    // ── legs → waypoints + mass budget ───────────────────────────────────────

    // A flight plan has one or more "legs", each leg has "segments".
    // Each segment has a "from" and a "to" position.
    // We flatten all segments into a single waypoints[] array.

    cJSON *legs = cJSON_GetObjectItemCaseSensitive(fp, "legs");
    int wp_index = 0;
    bool first_leg = true; // we only read fuel and payload from the first leg

    if (cJSON_IsArray(legs)) {
        cJSON *leg;
        cJSON_ArrayForEach(leg, legs) { // iterate over every leg in the array

            // ── mass budget (first leg only) ──────────────────────────────────
            if (first_leg) {
                double fuel_liters = 0.0;
                cJSON *fuel_obj = cJSON_GetObjectItemCaseSensitive(leg, "fuel");
                if (cJSON_IsObject(fuel_obj)) {
                    cJSON *qty  = cJSON_GetObjectItemCaseSensitive(fuel_obj, "quantity");
                    cJSON *unit = cJSON_GetObjectItemCaseSensitive(fuel_obj, "unit");
                    if (cJSON_IsNumber(qty)) {
                        // If JSON stores kg directly, use as-is; otherwise convert L → kg
                        if (cJSON_IsString(unit) && strcmp(unit->valuestring, "kg") == 0)
                            flight->initial_fuel_mass = qty->valuedouble;
                        else
                            flight->initial_fuel_mass = qty->valuedouble * FUEL_DENSITY_KG_PER_L;
                        fuel_liters = qty->valuedouble;
                        (void)fuel_liters; // suppress unused-variable warning
                    }
                }

                cJSON *payload_obj = cJSON_GetObjectItemCaseSensitive(leg, "payload");
                if (cJSON_IsObject(payload_obj)) {
                    cJSON *qty = cJSON_GetObjectItemCaseSensitive(payload_obj, "quantity");
                    if (cJSON_IsNumber(qty)) flight->payload = qty->valuedouble;
                }

                // Total take-off mass = structural + payload + fuel
                flight->initial_mass = flight->properties.empty_weight
                                       + flight->payload
                                       + flight->initial_fuel_mass;
                flight->current_mass = flight->initial_mass; // will decrease as fuel burns
                first_leg = false;
            }

            // ── waypoints from segments ───────────────────────────────────────

            cJSON *segments = cJSON_GetObjectItemCaseSensitive(leg, "segments");
            if (cJSON_IsArray(segments)) {
                cJSON *segment;
                cJSON_ArrayForEach(segment, segments) {
                    if (wp_index >= MAX_WAYPOINTS) break;

                    // Read the segment mode ("climb", "cruise", "descend")
                    FlightPhase seg_phase = PHASE_CLIMB;
                    cJSON *mode_json = cJSON_GetObjectItemCaseSensitive(segment, "mode");
                    if (cJSON_IsString(mode_json)) {
                        if (strcmp(mode_json->valuestring, "cruise") == 0)
                            seg_phase = PHASE_CRUISE;
                        else if (strcmp(mode_json->valuestring, "descend") == 0)
                            seg_phase = PHASE_DESCEND;
                    }

                    // For the very first segment, add the "from" position as
                    // waypoint 0 (the departure airport ground position).
                    // Also seed current_pos so the aircraft starts there.

                    if (wp_index == 0) {
                        cJSON *from = cJSON_GetObjectItemCaseSensitive(segment, "from");
                        if (cJSON_IsObject(from)) {
                            cJSON *lat = cJSON_GetObjectItemCaseSensitive(from, "latitude");
                            cJSON *lon = cJSON_GetObjectItemCaseSensitive(from, "longitude");
                            cJSON *alt = cJSON_GetObjectItemCaseSensitive(from, "altitude");
                            if (cJSON_IsNumber(lat)) {
                                flight->flight_plan.waypoints[wp_index].pos.latitude = lat->valuedouble;
                                flight->current_pos.latitude = lat->valuedouble;
                            }
                            if (cJSON_IsNumber(lon)) {
                                flight->flight_plan.waypoints[wp_index].pos.longitude = lon->valuedouble;
                                flight->current_pos.longitude = lon->valuedouble;
                            }
                            if (cJSON_IsNumber(alt)) {
                                flight->flight_plan.waypoints[wp_index].pos.altitude = alt->valuedouble;
                                flight->current_pos.altitude = alt->valuedouble;
                            }
                            flight->flight_plan.waypoints[wp_index].target_speed = 250.0;
                            flight->flight_plan.waypoints[wp_index].segment_phase = PHASE_CLIMB;
                            wp_index++;
                        }
                    }

                    if (wp_index >= MAX_WAYPOINTS) break;

                    // Every segment's "to" becomes the next waypoint, tagged with
                    // the segment's mode so the simulation knows the phase when
                    // flying toward this point.

                    cJSON *to = cJSON_GetObjectItemCaseSensitive(segment, "to");
                    if (cJSON_IsObject(to)) {
                        cJSON *lat = cJSON_GetObjectItemCaseSensitive(to, "latitude");
                        cJSON *lon = cJSON_GetObjectItemCaseSensitive(to, "longitude");
                        cJSON *alt = cJSON_GetObjectItemCaseSensitive(to, "altitude");
                        if (cJSON_IsNumber(lat))
                            flight->flight_plan.waypoints[wp_index].pos.latitude = lat->valuedouble;
                        if (cJSON_IsNumber(lon))
                            flight->flight_plan.waypoints[wp_index].pos.longitude = lon->valuedouble;
                        if (cJSON_IsNumber(alt))
                            flight->flight_plan.waypoints[wp_index].pos.altitude = alt->valuedouble;
                        flight->flight_plan.waypoints[wp_index].target_speed = 250.0;
                        flight->flight_plan.waypoints[wp_index].segment_phase = seg_phase;
                        wp_index++;
                    }
                }
            }
        }
    }

    flight->flight_plan.num_waypoints = wp_index;
    // Start heading toward waypoint 1 (top-of-climb); waypoint 0 is the departure
    // ground position where the aircraft already is.
    flight->flight_plan.current_waypoint_index = 1;

    printf("Loaded flight plan for %s with %d waypoints. "
           "empty=%.0f payload=%.0f fuel=%.0f initial_mass=%.0f kg\n",
           flight->callsign, flight->flight_plan.num_waypoints,
           flight->properties.empty_weight, flight->payload,
           flight->initial_fuel_mass, flight->initial_mass);

    shm->num_flights++; // advance the counter so the next call uses a fresh slot

    cJSON_Delete(json);
    free(data);
}

// ── Load all flight plans in a directory ──────────────────────────────────────

// Scans the given directory for .json files and calls parse_flight_plan() on each.
// The order depends on the filesystem — not necessarily alphabetical.

void load_all_flight_plans(const char* directory_path, SharedMemoryData* shm) {
    DIR *dir;
    struct dirent *ent; // one directory entry (file or subdirectory)

    if ((dir = opendir(directory_path)) != NULL) {
        while ((ent = readdir(dir)) != NULL) {
            // Check the filename ends with ".json" (length > 5 to skip ".json" itself)
            size_t len = strlen(ent->d_name);
            if (len > 5 && strcmp(ent->d_name + len - 5, ".json") == 0) {
                char filepath[1024];
                // Build the full path: directory_path + "/" + filename
                snprintf(filepath, sizeof(filepath), "%s/%s", directory_path, ent->d_name);
                parse_flight_plan(filepath, shm);
            }
        }
        closedir(dir); // always close the directory handle when done
    } else {
        perror("Could not open flightplansSim directory");
    }
}
