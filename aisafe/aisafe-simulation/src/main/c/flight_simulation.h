#ifndef FLIGHT_SIMULATION_H
#define FLIGHT_SIMULATION_H

#include "structs.h"

/**
 * @brief Entry point for a child flight process. Runs the step-lock loop until landing or termination.
 *
 * Installs SIGUSR1/SIGTERM handlers, signals the parent that it is ready,
 * then repeatedly reads a StepCommand from cmd_read_fd, computes one physics step,
 * and sends a PipeMessage with the full updated flight state to data_write_fd.
 * Exits when the aircraft lands, g_terminate is set, or StepCommand.terminate is true.
 *
 * @param shm           Pointer to the local simulation data block (read-only after fork).
 * @param flight_index  Index of this flight in shm->flights[].
 * @param data_write_fd Write end of the child→parent data pipe.
 * @param cmd_read_fd   Read end of the parent→child command pipe.
 */
void run_flight_process(SimulationData *shm, int flight_index,
                        int data_write_fd, int cmd_read_fd);

#endif /* FLIGHT_SIMULATION_H */
