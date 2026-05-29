#ifndef FLIGHT_SIMULATION_H
#define FLIGHT_SIMULATION_H

#include <semaphore.h>
#include "structs.h"

/**
 * @brief Entry point for a child flight process. Runs the step-lock loop until landing or termination.
 *
 * Installs SIGUSR1/SIGTERM handlers, signals the parent that it is ready,
 * then repeatedly waits on step_sem, computes one physics step, sends a PipeMessage
 * to the parent, and posts to main_sem. Exits when the aircraft lands,
 * g_terminate is set, or shm->termination_requested is true.
 *
 * @param shm           Pointer to the shared memory block.
 * @param flight_index  Index of this flight in shm->flights[].
 * @param pipe_write_fd Write end of the pipe to the parent process.
 * @param step_sem      Semaphore the parent posts to wake this process each step.
 * @param main_sem      Semaphore this process posts when a step is complete.
 */
void run_flight_process(SharedMemoryData *shm, int flight_index,
                        int pipe_write_fd, sem_t *step_sem, sem_t *main_sem);

#endif /* FLIGHT_SIMULATION_H */
