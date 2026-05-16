#ifndef FLIGHT_SIMULATION_H
#define FLIGHT_SIMULATION_H

#include <semaphore.h>
#include "structs.h"

// Entry point for each child process (one per flight).
// pipe_write_fd: write end of the pipe to the main process.
// step_sem:      child waits on this before each physics step (parent posts).
// main_sem:      child posts to this after each step (parent waits).
void run_flight_process(SharedMemoryData *shm, int flight_index,
                        int pipe_write_fd, sem_t *step_sem, sem_t *main_sem);

#endif /* FLIGHT_SIMULATION_H */
