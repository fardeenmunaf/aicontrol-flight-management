#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "environment.h"
#include "json_parser.h"

/** @State  module-level weather database, loaded once and shared across steps. */
static WeatherEntry g_weather_db[MAX_WEATHER_DB];
static int          g_weather_count = 0;
static bool         g_db_loaded     = false;

/**
 * @DisplayName("lookup_db_wind — spatial wind lookup against the weather database")
 *
 * @Given   g_weather_db contains zero or more WeatherEntry bounding boxes
 * @When    pos falls inside the lat/lon/alt range of entry i
 * @Then    out is populated with entry i's wind data and the function returns 1
 *
 * @When    no entry covers pos
 * @Then    out is left unchanged and the function returns 0
 *
 * @param pos  aircraft position to test (latitude, longitude, altitude)
 * @param out  destination WindData; written only on a hit
 * @return 1 on hit, 0 on miss
 */
static int lookup_db_wind(const Position *pos, WindData *out) {
    for (int i = 0; i < g_weather_count; i++) {
        WeatherEntry *e = &g_weather_db[i];
        double min_lat = e->lat1 < e->lat2 ? e->lat1 : e->lat2;
        double max_lat = e->lat1 > e->lat2 ? e->lat1 : e->lat2;
        double min_lon = e->lon1 < e->lon2 ? e->lon1 : e->lon2;
        double max_lon = e->lon1 > e->lon2 ? e->lon1 : e->lon2;

        if (pos->latitude  >= min_lat && pos->latitude  <= max_lat &&
            pos->longitude >= min_lon && pos->longitude <= max_lon &&
            pos->altitude  >= e->alt_inf && pos->altitude <= e->alt_sup) {
            out->wind_direction_deg = e->wind_direction_deg;
            out->wind_speed_ms      = e->wind_speed_ms;
            return 1;
        }
    }
    return 0;
}

/* ── Thread ─────────────────────────────────────────────────────────────── */

/**
 * @DisplayName("environment_thread — per-step wind update loop")
 *
 * @BeforeAll  loads weather_db.json into g_weather_db (once, on first entry)
 *
 * @Given   ctx->current_thread == THREAD_ENVIRONMENT and sim is not done
 * @When    the thread wakes from cond_environment
 * @Then    for every active flight, env_wind[i] is updated with the resolved
 *          wind (DB override if inside a known area, plan default otherwise)
 *          and control is handed to THREAD_DISPATCHER via cond_dispatch
 *
 * @When    ctx->sim_done is true
 * @Then    the thread exits cleanly without touching shared memory
 *
 * @param arg  pointer to SimContext cast from void*
 * @return NULL (pthread signature)
 */
void *environment_thread(void *arg) {
    SimContext *ctx = (SimContext *)arg;

    if (!g_db_loaded) {
        load_weather_db("weather_db.json", g_weather_db, MAX_WEATHER_DB, &g_weather_count);
        g_db_loaded = true;
    }

    while (1) {
        pthread_mutex_lock(&ctx->mutex);
        while (ctx->current_thread != THREAD_ENVIRONMENT && !ctx->sim_done)
            pthread_cond_wait(&ctx->cond_environment, &ctx->mutex);

        if (ctx->sim_done) {
            pthread_mutex_unlock(&ctx->mutex);
            break;
        }

        bool active[MAX_FLIGHTS];
        memcpy(active, ctx->was_active, sizeof(active));
        int step = ctx->step_num;
        pthread_mutex_unlock(&ctx->mutex);

        /* Update per-flight wind in shared memory. */
        sem_wait(SEM_ENV(ctx->shm));
        for (int i = 0; i < ctx->num_flights; i++) {
            if (!active[i]) continue;

            Flight *f  = &ctx->shm->sim.flights[i];
            int     wi = f->flight_plan.current_waypoint_index;

            /* Default: waypoint wind from the flight-plan JSON. */
            double spd = f->flight_plan.waypoints[wi].wind_speed_ms;
            double dir = f->flight_plan.waypoints[wi].wind_direction_deg;

            /* Override with live DB data if the aircraft is inside a known area. */
            WindData db;
            if (g_weather_count > 0 && lookup_db_wind(&f->current_pos, &db)) {
                spd = db.wind_speed_ms;
                dir = db.wind_direction_deg;
            }

            ctx->shm->env_wind[i].wind_speed_ms      = spd;
            ctx->shm->env_wind[i].wind_direction_deg  = dir;

            printf("[ENV] step %d  %s  wind %.1f m/s from %.0f°%s\n",
                   step, f->callsign, spd, dir,
                   (g_weather_count > 0 && lookup_db_wind(&f->current_pos, &db))
                       ? " (DB)" : " (plan)");
        }
        sem_post(SEM_ENV(ctx->shm));

        /* Hand control to the Dispatcher thread. */
        pthread_mutex_lock(&ctx->mutex);
        ctx->current_thread = THREAD_DISPATCHER;
        pthread_cond_signal(&ctx->cond_dispatch);
        pthread_mutex_unlock(&ctx->mutex);
    }

    return NULL;
}
