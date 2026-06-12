#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <dirent.h>
#include <sys/stat.h>
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
    if (cJSON_IsNumber(item)) config->end_time = item->valuedouble * 86400.0;

    item = cJSON_GetObjectItemCaseSensitive(json, "time_step");
    if (cJSON_IsNumber(item)) config->time_step = item->valuedouble * 60.0;

    item = cJSON_GetObjectItemCaseSensitive(json, "safety_distance_threshold");
    if (cJSON_IsNumber(item)) config->safety_distance_threshold = item->valuedouble;

    config->collision_distance_threshold = 100.0;
    item = cJSON_GetObjectItemCaseSensitive(json, "collision_distance_threshold");
    if (cJSON_IsNumber(item)) config->collision_distance_threshold = item->valuedouble;

    item = cJSON_GetObjectItemCaseSensitive(json, "max_violations_threshold");
    if (cJSON_IsNumber(item)) config->max_violations_threshold = item->valueint;

    item = cJSON_GetObjectItemCaseSensitive(json, "reroute");
    config->reroute = cJSON_IsTrue(item);

    item = cJSON_GetObjectItemCaseSensitive(json, "sim_start");
    if (cJSON_IsString(item)) {
        int sy = 0, sm = 0, sd = 0, h = 0, mi = 0, s = 0;
        sscanf(item->valuestring, "%d-%d-%dT%d:%d:%d", &sy, &sm, &sd, &h, &mi, &s);
        config->sim_start_seconds = h * 3600.0 + mi * 60.0 + s;
        config->sim_start_year   = sy;
        config->sim_start_month  = sm;
        config->sim_start_day    = sd;

        /* sim_start date must be >= today. */
        time_t now = time(NULL);
        struct tm *today = localtime(&now);
        int ty = today->tm_year + 1900;
        int tm_ = today->tm_mon + 1;
        int td = today->tm_mday;

        int sim_date  = sy * 10000 + sm * 100 + sd;
        int today_date = ty * 10000 + tm_ * 100 + td;

        if (sim_date < today_date) {
            fprintf(stderr, "[settings] sim_start date (%04d-%02d-%02d) is in the past — must be >= today (%04d-%02d-%02d)\n",
                    sy, sm, sd, ty, tm_, td);
            cJSON_Delete(json);
            free(data);
            return 0;
        }
    }

    cJSON_Delete(json);
    free(data);

    if (config->end_time <= config->start_time) {
        fprintf(stderr, "[settings] end_time (%.4f days) must be greater than start_time\n",
                config->end_time / 86400.0);
        return 0;
    }

    printf("Loaded settings: duration=%.2f days  step=%.0f min  warn=%.0f m  collision=%.0f m  max_viol=%d  sim_start=%02.0f:%02.0f  reroute=%s\n",
           config->end_time / 86400.0, config->time_step / 60.0,
           config->safety_distance_threshold, config->collision_distance_threshold,
           config->max_violations_threshold,
           floor(config->sim_start_seconds / 3600.0),
           fmod(config->sim_start_seconds, 3600.0) / 60.0,
           config->reroute ? "ON" : "OFF");
    return 1;
}

/* ── FlightProfile helpers ────────────────────────────────────────────────── */

#define MAX_PROFILE_ENTRIES 10

typedef struct { double alt_m; double spd_kts; double rate_mps; } ProfileEntry;

/* Returns the speed (kts) from the profile for the given phase and altitude.
 * Returns 0.0 when no profile data is available for that phase so the caller
 * can fall back to the hardcoded ATC speed schedule. */
static double profile_speed_kts(FlightPhase phase, double alt_m,
                                 const ProfileEntry *climb, int n_climb,
                                 double cruise_kts,
                                 const ProfileEntry *desc,  int n_desc) {
    if (phase == PHASE_CRUISE)
        return cruise_kts;

    const ProfileEntry *arr = (phase == PHASE_CLIMB) ? climb : desc;
    int n                   = (phase == PHASE_CLIMB) ? n_climb : n_desc;
    if (n == 0) return 0.0;

    int best = 0;
    double best_diff = fabs(arr[0].alt_m - alt_m);
    for (int i = 1; i < n; i++) {
        double diff = fabs(arr[i].alt_m - alt_m);
        if (diff < best_diff) { best_diff = diff; best = i; }
    }
    return arr[best].spd_kts;
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
/**
 * @brief Returns the number of days from (y1,m1,d1) to (y2,m2,d2) using Julian Day Numbers.
 *
 * Negative result means date2 is before date1.
 */
static int days_between(int y1, int m1, int d1, int y2, int m2, int d2) {
    int a1 = (14 - m1) / 12, a2 = (14 - m2) / 12;
    int jd1 = d1 + (153*(m1 + 12*a1 - 3) + 2)/5 + 365*(y1 - a1)
              + (y1 - a1)/4 - (y1 - a1)/100 + (y1 - a1)/400 - 32045;
    int jd2 = d2 + (153*(m2 + 12*a2 - 3) + 2)/5 + 365*(y2 - a2)
              + (y2 - a2)/4 - (y2 - a2)/100 + (y2 - a2)/400 - 32045;
    return jd2 - jd1;
}

static int parse_flight_plan(const char* filepath, SimulationData* shm) {
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
                cJSON *dep_date = cJSON_GetObjectItemCaseSensitive(dep_obj, "date");
                cJSON *dep_time = cJSON_GetObjectItemCaseSensitive(dep_obj, "time");
                if (cJSON_IsString(dep_time)) {
                    int dh = 0, dm = 0;
                    if (sscanf(dep_time->valuestring, "%d:%d", &dh, &dm) == 2) {
                        departure_s = dh * 3600.0 + dm * 60.0;
                        double day_offset_s = 0.0;
                        if (cJSON_IsString(dep_date)) {
                            int dy = 0, dmo = 0, dd = 0;
                            if (sscanf(dep_date->valuestring, "%d-%d-%d", &dy, &dmo, &dd) == 3) {
                                day_offset_s = days_between(
                                    shm->config.sim_start_year, shm->config.sim_start_month, shm->config.sim_start_day,
                                    dy, dmo, dd) * 86400.0;
                            }
                        }
                        cur_leg->departure_offset_s = day_offset_s + departure_s - shm->config.sim_start_seconds;
                    }
                }
            }

            cJSON *arr_obj = cJSON_GetObjectItemCaseSensitive(leg, "arrival");
            if (cJSON_IsObject(arr_obj)) {
                cJSON *arr_date = cJSON_GetObjectItemCaseSensitive(arr_obj, "date");
                cJSON *arr_time = cJSON_GetObjectItemCaseSensitive(arr_obj, "time");
                if (cJSON_IsString(arr_time)) {
                    int ah = 0, am = 0;
                    if (sscanf(arr_time->valuestring, "%d:%d", &ah, &am) == 2) {
                        double arrive_s = ah * 3600.0 + am * 60.0;
                        double arr_day_offset_s = 0.0;
                        if (cJSON_IsString(arr_date)) {
                            int ay = 0, amo = 0, ad = 0;
                            if (sscanf(arr_date->valuestring, "%d-%d-%d", &ay, &amo, &ad) == 3) {
                                arr_day_offset_s = days_between(
                                    shm->config.sim_start_year, shm->config.sim_start_month, shm->config.sim_start_day,
                                    ay, amo, ad) * 86400.0;
                            }
                        }
                        double arrival_offset = arr_day_offset_s + arrive_s - shm->config.sim_start_seconds;
                        if (departure_s >= 0.0 && arrival_offset <= cur_leg->departure_offset_s) {
                            fprintf(stderr, "[%s] Leg %d: arrival must be after departure\n", label, leg_index);
                            valid = 0;
                        } else {
                            cur_leg->scheduled_arrival_offset_s = arrival_offset;
                        }
                    }
                }
            }

            // ── FlightProfile: speed schedule per phase ────────────────────────
            ProfileEntry climb_prof[MAX_PROFILE_ENTRIES];
            int n_climb    = 0;
            double cruise_kts = 0.0;
            ProfileEntry desc_prof[MAX_PROFILE_ENTRIES];
            int n_desc     = 0;

            cJSON *profile_json = cJSON_GetObjectItemCaseSensitive(leg, "flightProfile");
            if (cJSON_IsObject(profile_json)) {
                cJSON *e;
                cJSON *climb_arr = cJSON_GetObjectItemCaseSensitive(profile_json, "climb");
                if (cJSON_IsArray(climb_arr)) {
                    cJSON_ArrayForEach(e, climb_arr) {
                        if (n_climb >= MAX_PROFILE_ENTRIES) break;
                        cJSON *a  = cJSON_GetObjectItemCaseSensitive(e, "altitude");
                        cJSON *s  = cJSON_GetObjectItemCaseSensitive(e, "speed");
                        cJSON *av = a ? cJSON_GetObjectItemCaseSensitive(a, "value") : NULL;
                        cJSON *sv = s ? cJSON_GetObjectItemCaseSensitive(s, "value") : NULL;
                        if (cJSON_IsNumber(av) && cJSON_IsNumber(sv)) {
                            climb_prof[n_climb].alt_m   = av->valuedouble;
                            climb_prof[n_climb].spd_kts = sv->valuedouble;
                            cJSON *rc  = cJSON_GetObjectItemCaseSensitive(e, "rateClimb");
                            cJSON *rcv = rc ? cJSON_GetObjectItemCaseSensitive(rc, "value") : NULL;
                            climb_prof[n_climb].rate_mps = cJSON_IsNumber(rcv) ? rcv->valuedouble : 0.0;
                            n_climb++;
                        }
                    }
                }
                cJSON *cruise_obj = cJSON_GetObjectItemCaseSensitive(profile_json, "cruise");
                if (cJSON_IsObject(cruise_obj)) {
                    cJSON *s  = cJSON_GetObjectItemCaseSensitive(cruise_obj, "speed");
                    cJSON *sv = s ? cJSON_GetObjectItemCaseSensitive(s, "value") : NULL;
                    if (cJSON_IsNumber(sv))
                        cruise_kts = sv->valuedouble;
                }
                cJSON *desc_arr = cJSON_GetObjectItemCaseSensitive(profile_json, "descend");
                if (cJSON_IsArray(desc_arr)) {
                    cJSON_ArrayForEach(e, desc_arr) {
                        if (n_desc >= MAX_PROFILE_ENTRIES) break;
                        cJSON *a  = cJSON_GetObjectItemCaseSensitive(e, "altitude");
                        cJSON *s  = cJSON_GetObjectItemCaseSensitive(e, "speed");
                        cJSON *av = a ? cJSON_GetObjectItemCaseSensitive(a, "value") : NULL;
                        cJSON *sv = s ? cJSON_GetObjectItemCaseSensitive(s, "value") : NULL;
                        if (cJSON_IsNumber(av) && cJSON_IsNumber(sv)) {
                            desc_prof[n_desc].alt_m   = av->valuedouble;
                            desc_prof[n_desc].spd_kts = sv->valuedouble;
                            cJSON *rd  = cJSON_GetObjectItemCaseSensitive(e, "rateDescent");
                            cJSON *rdv = rd ? cJSON_GetObjectItemCaseSensitive(rd, "value") : NULL;
                            desc_prof[n_desc].rate_mps = cJSON_IsNumber(rdv) ? rdv->valuedouble : 0.0;
                            n_desc++;
                        }
                    }
                }
            }

            /* Aggregate climb/descent rate caps and store on Flight. */
            double max_climb = 0.0, min_desc = 0.0;
            for (int pi = 0; pi < n_climb; pi++)
                if (climb_prof[pi].rate_mps > max_climb)
                    max_climb = climb_prof[pi].rate_mps;
            for (int pi = 0; pi < n_desc; pi++)
                if (desc_prof[pi].rate_mps < 0.0 && desc_prof[pi].rate_mps < min_desc)
                    min_desc = desc_prof[pi].rate_mps;
            flight->max_climb_rate_mps   = max_climb;
            flight->max_descent_rate_mps = min_desc;

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

                    // Parse wind for this segment (stored on every waypoint it produces).
                    double seg_wind_dir = 0.0, seg_wind_spd = 0.0;
                    cJSON *wind_json = cJSON_GetObjectItemCaseSensitive(segment, "wind");
                    if (cJSON_IsObject(wind_json)) {
                        cJSON *dir     = cJSON_GetObjectItemCaseSensitive(wind_json, "direction");
                        cJSON *spd_obj = cJSON_GetObjectItemCaseSensitive(wind_json, "speed");
                        cJSON *spd_val = spd_obj ? cJSON_GetObjectItemCaseSensitive(spd_obj, "value") : NULL;
                        if (cJSON_IsNumber(dir))     seg_wind_dir = dir->valuedouble;
                        if (cJSON_IsNumber(spd_val)) seg_wind_spd = spd_val->valuedouble;
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
                            flight->flight_plan.waypoints[wp_index].target_speed      =
                                profile_speed_kts(PHASE_CLIMB, alt, climb_prof, n_climb, cruise_kts, desc_prof, n_desc);
                            flight->flight_plan.waypoints[wp_index].wind_direction_deg = seg_wind_dir;
                            flight->flight_plan.waypoints[wp_index].wind_speed_ms      = seg_wind_spd;
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
                        flight->flight_plan.waypoints[wp_index].target_speed  =
                            profile_speed_kts(seg_phase, alt, climb_prof, n_climb, cruise_kts, desc_prof, n_desc);
                        flight->flight_plan.waypoints[wp_index].wind_direction_deg = seg_wind_dir;
                        flight->flight_plan.waypoints[wp_index].wind_speed_ms      = seg_wind_spd;
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
int load_all_flight_plans(const char* directory_path, SimulationData* shm) {
    struct dirent **entries;
    int all_valid = 1;

    /* scandir with alphasort guarantees alphabetical order so the menu numbering
       is deterministic and matches any external tool that lists the same directory. */
    int n = scandir(directory_path, &entries, NULL, alphasort);
    if (n < 0) {
        perror("Could not open flightplansSim directory");
        return 0;
    }

    for (int i = 0; i < n; i++) {
        const char *name = entries[i]->d_name;
        size_t len = strlen(name);
        if (len > 5 && strcmp(name + len - 5, ".json") == 0) {
            char filepath[1024];
            snprintf(filepath, sizeof(filepath), "%s/%s", directory_path, name);
            if (!parse_flight_plan(filepath, shm))
                all_valid = 0;
        }
        free(entries[i]);
    }
    free(entries);

    return all_valid;
}
