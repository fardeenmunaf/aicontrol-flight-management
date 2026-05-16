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
    int leg_index = 0;

    if (cJSON_IsArray(legs)) {
        cJSON *leg;
        cJSON_ArrayForEach(leg, legs) {
            if (leg_index >= MAX_LEGS) {
                fprintf(stderr, "[%s] Too many legs (max %d). Skipping remaining.\n",
                        flight->callsign, MAX_LEGS);
                break;
            }

            Leg *cur_leg = &flight->legs[leg_index];

            // ── fuel ──────────────────────────────────────────────────────────
            cJSON *fuel_obj = cJSON_GetObjectItemCaseSensitive(leg, "fuel");
            if (cJSON_IsObject(fuel_obj)) {
                cJSON *qty  = cJSON_GetObjectItemCaseSensitive(fuel_obj, "quantity");
                cJSON *unit = cJSON_GetObjectItemCaseSensitive(fuel_obj, "unit");
                if (cJSON_IsNumber(qty)) {
                    if (cJSON_IsString(unit) && strcmp(unit->valuestring, "kg") == 0)
                        cur_leg->fuel_mass = qty->valuedouble;
                    else
                        cur_leg->fuel_mass = qty->valuedouble * FUEL_DENSITY_KG_PER_L;
                }
            }

            // ── payload ───────────────────────────────────────────────────────
            cJSON *payload_obj = cJSON_GetObjectItemCaseSensitive(leg, "payload");
            if (cJSON_IsObject(payload_obj)) {
                cJSON *qty = cJSON_GetObjectItemCaseSensitive(payload_obj, "quantity");
                if (cJSON_IsNumber(qty)) cur_leg->payload = qty->valuedouble;
            }

            // ── departure offset ──────────────────────────────────────────────
            cJSON *dep_obj = cJSON_GetObjectItemCaseSensitive(leg, "departure");
            if (cJSON_IsObject(dep_obj)) {
                cJSON *dep_time = cJSON_GetObjectItemCaseSensitive(dep_obj, "time");
                if (cJSON_IsString(dep_time)) {
                    int dh = 0, dm = 0;
                    sscanf(dep_time->valuestring, "%d:%d", &dh, &dm);
                    double depart_s = dh * 3600.0 + dm * 60.0;
                    cur_leg->departure_offset_s = depart_s - shm->config.sim_start_seconds;
                }
            }

            // ── waypoints from segments ───────────────────────────────────────
            // waypoint_start_index: for leg 0, it is 1 (we skip the ground departure
            // waypoint at index 0 and start navigating toward the top-of-climb).
            // For subsequent legs the departure point is already the last waypoint of
            // the previous leg, so no "from" is added and start_index = current wp_index.
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

                    // Add the "from" position only for the very first segment of leg 0.
                    // That seeds current_pos and waypoint[0] (the departure ground position).
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

            // Record the waypoint slice owned by this leg.
            // Leg 0: start=1 (skip the ground departure at index 0), end=wp_index-1
            // Leg N: start=leg_wp_start_before (first "to" added for this leg), end=wp_index-1
            cur_leg->waypoint_start_index = (leg_index == 0) ? 1 : leg_wp_start_before;
            cur_leg->waypoint_end_index   = wp_index - 1;

            leg_index++;
        }
    }

    flight->num_legs    = leg_index;
    flight->current_leg = 0;
    flight->leg_just_completed = false;

    // Bootstrap mass budget and departure from leg 0
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
