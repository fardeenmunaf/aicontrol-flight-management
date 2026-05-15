#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include "cJSON.h"
#include "json_parser.h"

// Helper to read file contents into a string
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

// Parse simulation settings
void load_simulation_settings(const char* filename, SimulationConfig* config) {
    char* data = read_file(filename);
    if (!data) {
        fprintf(stderr, "Could not open %s\n", filename);
        return;
    }
    
    cJSON *json = cJSON_Parse(data);
    if (!json) {
        fprintf(stderr, "Error parsing JSON in %s\n", filename);
        free(data);
        return;
    }
    
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
    
    cJSON_Delete(json);
    free(data);
    printf("Loaded settings: start=%.1f end=%.1f step=%.1f dist=%.1f max_viol=%d\n",
           config->start_time, config->end_time, config->time_step,
           config->safety_distance_threshold, config->max_violations_threshold);
}

// Parse a single flight plan JSON file into the shared memory flights array
static void parse_flight_plan(const char* filepath, SharedMemoryData* shm) {
    char* data = read_file(filepath);
    if (!data) return;
    
    cJSON *json = cJSON_Parse(data);
    if (!json) {
        fprintf(stderr, "Error parsing flight plan JSON in %s\n", filepath);
        free(data);
        return;
    }
    
    cJSON *fp = cJSON_IsArray(json) ? cJSON_GetArrayItem(json, 0) : json;
    if (!fp) {
        cJSON_Delete(json);
        free(data);
        return;
    }
    
    if (shm->num_flights >= MAX_FLIGHTS) {
        fprintf(stderr, "Max flights reached. Skipping %s\n", filepath);
        cJSON_Delete(json);
        free(data);
        return;
    }
    
    Flight *flight = &shm->flights[shm->num_flights];
    flight->id = shm->num_flights + 1;
    flight->is_active = true;
    flight->violation_detected = false;
    
    cJSON *designator = cJSON_GetObjectItemCaseSensitive(fp, "flightDesignator");
    if (cJSON_IsString(designator)) {
        strncpy(flight->callsign, designator->valuestring, CALLSIGN_LEN - 1);
        flight->callsign[CALLSIGN_LEN - 1] = '\0';
    }
    
    cJSON *legs = cJSON_GetObjectItemCaseSensitive(fp, "legs");
    int wp_index = 0;
    if (cJSON_IsArray(legs)) {
        cJSON *leg;
        cJSON_ArrayForEach(leg, legs) {
            cJSON *segments = cJSON_GetObjectItemCaseSensitive(leg, "segments");
            if (cJSON_IsArray(segments)) {
                cJSON *segment;
                cJSON_ArrayForEach(segment, segments) {
                    if (wp_index >= MAX_WAYPOINTS) break;
                    
                    cJSON *to = cJSON_GetObjectItemCaseSensitive(segment, "to");
                    if (cJSON_IsObject(to)) {
                        cJSON *lat = cJSON_GetObjectItemCaseSensitive(to, "latitude");
                        cJSON *lon = cJSON_GetObjectItemCaseSensitive(to, "longitude");
                        cJSON *alt = cJSON_GetObjectItemCaseSensitive(to, "altitude");
                        
                        if (cJSON_IsNumber(lat)) flight->flight_plan.waypoints[wp_index].pos.latitude = lat->valuedouble;
                        if (cJSON_IsNumber(lon)) flight->flight_plan.waypoints[wp_index].pos.longitude = lon->valuedouble;
                        if (cJSON_IsNumber(alt)) flight->flight_plan.waypoints[wp_index].pos.altitude = alt->valuedouble;
                        
                        flight->flight_plan.waypoints[wp_index].target_speed = 250.0;
                        wp_index++;
                    }
                }
            }
        }
    }
    flight->flight_plan.num_waypoints = wp_index;
    flight->flight_plan.current_waypoint_index = 0;
    
    printf("Loaded flight plan for %s with %d waypoints.\n", flight->callsign, flight->flight_plan.num_waypoints);
    
    shm->num_flights++;
    
    cJSON_Delete(json);
    free(data);
}

// Load all flight plans from the flightplans directory
void load_all_flight_plans(const char* directory_path, SharedMemoryData* shm) {
    DIR *dir;
    struct dirent *ent;
    if ((dir = opendir(directory_path)) != NULL) {
        while ((ent = readdir(dir)) != NULL) {
            size_t len = strlen(ent->d_name);
            if (len > 5 && strcmp(ent->d_name + len - 5, ".json") == 0) {
                char filepath[1024];
                snprintf(filepath, sizeof(filepath), "%s/%s", directory_path, ent->d_name);
                parse_flight_plan(filepath, shm);
            }
        }
        closedir(dir);
    } else {
        perror("Could not open flightplans directory");
    }
}
