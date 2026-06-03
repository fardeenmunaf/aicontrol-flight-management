#ifndef FLIGHT_SIMULATION_H
#define FLIGHT_SIMULATION_H

#include "structs.h"

/**
 * @brief Entry point for a child flight process.
 *
 * Installs SIGUSR1/SIGTERM handlers, signals the parent via step_done[flight_index],
 * then loops: sem_wait(step_go) → compute physics step → write state into shm → sem_post(step_done).
 * Exits when the aircraft lands, g_terminate is set, or shm->terminate is true.
 *
 * @param shm          Pointer to the shared memory block (inherited via fork+mmap).
 * @param flight_index Index of this flight in shm->sim.flights[].
 */
void run_flight_process(SharedBlock *shm, int flight_index);

#endif /* FLIGHT_SIMULATION_H */
