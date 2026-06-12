#ifndef JSON_PARSER_H
#define JSON_PARSER_H

#include "structs.h"

/**
 * @brief Parses simulation-wide settings from a JSON file into a SimulationConfig.
 *
 * Must be called before any flight plans are loaded.
 *
 * @param filename Path to the simulation settings JSON file.
 * @param config   Output struct to populate.
 * @return 1 on success, 0 if the file cannot be read, parsed, or contains invalid values.
 */
int load_simulation_settings(const char* filename, SimulationConfig* config);

/**
 * @brief Scans a directory for .json files and loads each as a flight plan.
 *
 * Calls parse_flight_plan() for every file whose name ends in ".json".
 * Flights are appended to shm->flights[].
 *
 * @param directory_path Path to the directory containing flight plan JSON files.
 * @param shm            Pointer to shared memory to populate.
 * @return 1 if all flight plans loaded successfully, 0 if any failed validation.
 */
int load_all_flight_plans(const char* directory_path, SimulationData* shm);

/**
 * @brief Loads weather bounding-box records from a JSON file into a caller-supplied array.
 *
 * Missing or unparseable file is not fatal — returns 1 with *count == 0 so the
 * simulation falls back to flight-plan wind values.
 *
 * @param path       Path to the weather DB JSON file.
 * @param db         Caller-allocated array of WeatherEntry to fill.
 * @param max_entries Capacity of db[].
 * @param count      Out: number of records written.
 * @return Always 1 (errors are non-fatal and logged to stdout).
 */
int load_weather_db(const char *path, WeatherEntry *db, int max_entries, int *count);

#endif /* JSON_PARSER_H */
