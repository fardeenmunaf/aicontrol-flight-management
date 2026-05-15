#ifndef JSON_PARSER_H
#define JSON_PARSER_H

#include "structs.h"

// Parse simulation settings from the given file into the config struct
void load_simulation_settings(const char* filename, SimulationConfig* config);

// Load all flight plans from the specified directory into the shared memory flights array
void load_all_flight_plans(const char* directory_path, SharedMemoryData* shm);

#endif /* JSON_PARSER_H */
