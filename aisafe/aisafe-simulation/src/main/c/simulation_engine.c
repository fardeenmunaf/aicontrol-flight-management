#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "structs.h"
#include "json_parser.h"

int main(int argc, char **argv) {
    (void)argc;
    (void)argv;
    
    printf("Starting AISafe Simulation Engine...\n");
    
    SharedMemoryData shm_data;
    memset(&shm_data, 0, sizeof(SharedMemoryData));
    
    // Load simulation settings
    load_simulation_settings("simulation_settings.json", &shm_data.config);
    
    // Load all flight plans
    load_all_flight_plans("flightplans", &shm_data);
    
    printf("Initialization complete. %d flights loaded.\n", shm_data.num_flights);
    
    // TODO: Fork worker processes and start simulation loop
    
    return EXIT_SUCCESS;
}
