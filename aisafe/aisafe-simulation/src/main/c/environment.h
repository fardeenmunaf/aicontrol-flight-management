#ifndef ENVIRONMENT_H
#define ENVIRONMENT_H

#include "structs.h"

/**
 * @brief Environment thread entry point (US110).
 *
 * Runs as a parent-process pthread alongside the other simulation threads.
 * At each time step (THREAD_ENVIRONMENT phase) it updates env_wind[i] in
 * shared memory for every active flight before the Dispatcher wakes the
 * child processes.
 *
 * Wind source priority:
 *   1. weather_db.json exported from the Java weather service — if the
 *      aircraft's current position falls inside a WeatherData bounding box
 *      and altitude range, that record's values are used.
 *   2. Fallback: wind_speed_ms / wind_direction_deg already stored on the
 *      current waypoint (loaded from the flight-plan JSON at parse time).
 *
 * If weather_db.json is absent or unreadable, the thread applies calm
 * conditions (0 m/s) as a safe default and logs the situation.
 *
 * @param arg Pointer to SimContext (cast from void *).
 * @return NULL
 */
void *environment_thread(void *arg);

#endif /* ENVIRONMENT_H */
