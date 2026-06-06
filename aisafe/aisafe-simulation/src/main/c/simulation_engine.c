#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <semaphore.h>
#include <pthread.h>
#include <unistd.h>
#include <signal.h>
#include "structs.h"
#include "json_parser.h"
#include "flight_simulation.h"
#include "position_tracker.h"
#include "physics.h"

// ── Violation detection ────────────────────────────────────────────────────────

/**
 * @brief Checks all active flight pairs for proximity violations and all flights for fuel exhaustion.
 *
 * Sends SIGUSR1 to each aircraft involved in a proximity violation.
 * If total violations exceed config.max_violations_threshold, sends SIGTERM to all
 * active flights and sets sim->termination_requested.
 *
 * @param sim        Pointer to the local simulation data block.
 * @param was_active Boolean array indicating which flights were active at step start.
 * @param pids       Array of child PIDs indexed by flight slot.
 * @param num_flights Total number of flight slots.
 */
static void check_violations(SimulationData *sim, bool *was_active,
                              pid_t *pids, int num_flights) {
    /* Clear deviations each step; the lookahead below re-sets them for active conflicts. */
    for (int i = 0; i < num_flights; i++)
        sim->deviation_rad[i] = 0.0;

    for (int i = 0; i < sim->num_flights; i++) {
        if (!was_active[i] || !sim->flights[i].is_active) continue;

        for (int j = i + 1; j < sim->num_flights; j++) {
            if (!was_active[j] || !sim->flights[j].is_active) continue;

            double dist = min_separation_m(&sim->prev_pos[i], &sim->flights[i].current_pos,
                                           &sim->prev_pos[j], &sim->flights[j].current_pos);

            if (dist < sim->config.collision_distance_threshold) {
                /* Hard collision — kill both. */
                sim->total_violations_detected++;
                sim->flights[i].violation_detected = true;
                sim->flights[j].violation_detected = true;

                if (sim->report.total_violations_recorded < MAX_RECORDED_VIOLATIONS) {
                    ViolationEvent *evt = &sim->report.violations[sim->report.total_violations_recorded];
                    evt->timestamp   = sim->current_time;
                    evt->flight_a_id = i;
                    evt->flight_b_id = j;
                    evt->pos_a       = sim->flights[i].current_pos;
                    evt->pos_b       = sim->flights[j].current_pos;
                    evt->vel_a       = sim->flights[i].velocity;
                    evt->vel_b       = sim->flights[j].velocity;
                    evt->distance    = dist;
                    sim->report.total_violations_recorded++;
                }

                printf("  *** COLLISION: %s <-> %s  dist=%.1f m — both flights destroyed ***\n",
                       sim->flights[i].callsign, sim->flights[j].callsign, dist);
                if (pids[i] > 0) {
                    kill(pids[i], SIGTERM);
                    sim->flights[i].is_active = false;
                    pids[i] = 0;
                }
                if (pids[j] > 0) {
                    kill(pids[j], SIGTERM);
                    sim->flights[j].is_active = false;
                    pids[j] = 0;
                }

            } else if (dist < sim->config.safety_distance_threshold) {
                printf("  *** WARNING: %s <-> %s  dist=%.0f m (threshold %.0f m) ***\n",
                       sim->flights[i].callsign, sim->flights[j].callsign,
                       dist, sim->config.safety_distance_threshold);
            }
        }
    }

    for (int i = 0; i < sim->num_flights; i++) {
        if (!was_active[i]) continue;
        Flight *f = &sim->flights[i];
        double fuel_rem = f->current_mass - f->properties.empty_weight - f->payload;
        if (fuel_rem <= 0.0 && !f->fuel_exhaustion_reported) {
            f->fuel_exhaustion_reported = true;
            f->violation_detected       = true;
            sim->total_violations_detected++;
            printf("  *** VIOLATION: %s fuel exhausted — flight destroyed ***\n", f->callsign);
            if (pids[i] > 0) {
                kill(pids[i], SIGTERM);
                f->is_active = false;
                pids[i] = 0;
            }
        }
    }

    /* Reroute lookahead: project each active pair 500 km along their intent heading
       (bearing to destination, before deviation) and measure the closest they would
       get. If that is under 2x the safety threshold, both aircraft turn right 15deg
       (ICAO head-on rule) so they pass on opposite sides. Projecting along intent —
       not the already-deviated heading — keeps the check honest: it always answers
       "would they collide if we stopped deviating now?", so the turn isn't dropped
       prematurely. Stateless: recomputed fresh every step. */
    if (sim->config.reroute) {
        const double LOOKAHEAD_M = 500000.0;
        const double reroute_threshold = 2.0 * sim->config.safety_distance_threshold;
        for (int i = 0; i < sim->num_flights; i++) {
            if (!was_active[i] || !sim->flights[i].is_active) continue;

            Position future_i = advance_pos(sim->flights[i].current_pos,
                                            sim->flights[i].intent_heading, LOOKAHEAD_M);

            for (int j = i + 1; j < sim->num_flights; j++) {
                if (!was_active[j] || !sim->flights[j].is_active) continue;

                Position future_j = advance_pos(sim->flights[j].current_pos,
                                                sim->flights[j].intent_heading, LOOKAHEAD_M);

                double pred = min_separation_m(&sim->flights[i].current_pos, &future_i,
                                               &sim->flights[j].current_pos, &future_j);

                if (pred < reroute_threshold) {
                    printf("  *** AVOIDANCE: %s <-> %s predicted %.0f m within 500 km — both turn right ***\n",
                           sim->flights[i].callsign, sim->flights[j].callsign, pred);
                    sim->deviation_rad[i] = M_PI / 12.0;
                    sim->deviation_rad[j] = M_PI / 12.0;
                }
            }
        }
    }

    if (sim->config.max_violations_threshold > 0 &&
        sim->total_violations_detected > sim->config.max_violations_threshold) {
        printf("  [SIM] Violations (%d) exceeded threshold (%d) — "
               "sending SIGTERM to all active flights.\n",
               sim->total_violations_detected, sim->config.max_violations_threshold);
        for (int i = 0; i < num_flights; i++)
            if (pids[i] > 0 && sim->flights[i].is_active)
                kill(pids[i], SIGTERM);
        sim->termination_requested = true;
    }
}

// ── Step output ────────────────────────────────────────────────────────────────

static const char *phase_name(FlightPhase p) {
    switch (p) {
        case PHASE_GATE:    return "GATE   ";
        case PHASE_CLIMB:   return "CLIMB  ";
        case PHASE_CRUISE:  return "CRUISE ";
        case PHASE_DESCEND: return "DESCEND";
        default:            return "???????";
    }
}

/**
 * @brief Prints scheduled vs. actual arrival time and deviation for one flight leg.
 *
 * @param scheduled_offset_s Scheduled arrival offset in seconds, or negative if unset.
 * @param actual_offset_s    Actual arrival offset in seconds.
 * @param sim_start_s        Wall-clock anchor in seconds since midnight.
 */
static void print_arrival_deviation(double scheduled_offset_s,double actual_offset_s,
    double sim_start_s) {
    if (scheduled_offset_s < 0.0) return;
    int dev_min    = (int)((actual_offset_s - scheduled_offset_s) / 60.0);
    int sched_wall = (int)(sim_start_s + scheduled_offset_s);
    int act_wall   = (int)(sim_start_s + actual_offset_s);
    const char *label = dev_min > 0 ? "LATE" : dev_min < 0 ? "EARLY" : "on time";
    printf("         Sched: %02d:%02d | Actual: %02d:%02d | Deviation: %+d min (%d min %s)\n",
           sched_wall / 3600, (sched_wall % 3600) / 60,
           act_wall   / 3600, (act_wall   % 3600) / 60,
           dev_min, abs(dev_min), label);
}

/**
 * @brief Prints the full 25-column physics table for all active flights after one step.
 *
 * @param sim        Pointer to the local simulation data block.
 * @param msgs       PipeMessage array from the current step (indexed by flight slot).
 * @param prev_ph    Previous phase per flight (used to detect phase transitions).
 * @param sim_time   Simulation clock after advancing.
 * @param step_num   Current step number (1-based).
 * @param total_steps Total number of steps in the simulation.
 * @param step_time  Simulation clock before advancing.
 * @param was_active Boolean array indicating which flights were active at step start.
 */
static void print_step_table(SimulationData *sim,
                              FlightPhase *prev_ph,
                              double sim_time,
                              int step_num, int total_steps,
                              double step_time,
                              bool *was_active) {
    int active = 0, pending = 0;
    for (int i = 0; i < sim->num_flights; i++) {
        if (sim->flights[i].is_active && step_time >= sim->flights[i].departure_offset_s) active++;
        if (sim->flights[i].is_active && step_time <  sim->flights[i].departure_offset_s) pending++;
    }

    int wall_s = (int)(sim->config.sim_start_seconds + sim_time);
    struct tm wall_tm = {0};
    wall_tm.tm_year = sim->config.sim_start_year - 1900;
    wall_tm.tm_mon  = sim->config.sim_start_month - 1;
    wall_tm.tm_mday = sim->config.sim_start_day + wall_s / 86400;
    wall_tm.tm_hour = (wall_s % 86400) / 3600;
    wall_tm.tm_min  = (wall_s % 3600) / 60;
    mktime(&wall_tm);
    printf("\n╔══════════════════════════════════════════════════════════════════════"
           "════════════════════════════════════════════════════════════════════╗\n");
    printf("║  t=%7.0f s  [%02d/%02d/%04d %02d:%02d]  |  step %3d / %-3d  |  active=%2d  pending=%2d  |  violations=%3d"
           "                              ║\n",
           sim_time,
           wall_tm.tm_mday, wall_tm.tm_mon + 1, wall_tm.tm_year + 1900,
           wall_tm.tm_hour, wall_tm.tm_min,
           step_num, total_steps, active, pending, sim->total_violations_detected);
    printf("╚══════════════════════════════════════════════════════════════════════"
           "════════════════════════════════════════════════════════════════════╝\n");

    for (int i = 0; i < sim->num_flights; i++) {
        if (!was_active[i]) continue;
        FlightPhase cur = sim->last_step[i].phase;
        if (cur != prev_ph[i])
            printf("  >>> %s: %s --> %s <<<\n",
                   sim->flights[i].callsign, phase_name(prev_ph[i]), phase_name(cur));
    }

    printf("\n%-8s %-7s %8s %9s %7s %8s %5s %6s %8s %9s %6s  %6s %5s %7s  %6s %6s  %8s %8s  %8s %8s  %7s  %6s %7s  %6s %6s\n",
           "CALLSGN","PHASE","LAT(deg)","LON(deg)","ALT(m)","DEST(km)","CA(m)",
           "T(K)","P(Pa)","rho","a(m/s)","IAS(kt)","Mach","TAS(m/s)",
           "CL","CD","Lift(N)","Drag(N)","Te(N)","Tt(N)","VS(m/s)",
           "dF(kg)","dD(km)","M(kg)","Frem(kg)");
    printf("%-8s %-7s %8s %9s %7s %8s %5s %6s %8s %9s %6s  %6s %5s %7s  %6s %6s  %8s %8s  %8s %8s  %7s  %6s %7s  %6s %6s\n",
           "--------","-------","--------","---------","-------","--------","-----",
           "------","--------","---------","------","------","-----","-------",
           "------","------","--------","--------","--------","--------","-------",
           "------","-------","------","-------");

    for (int i = 0; i < sim->num_flights; i++) {
        if (!was_active[i]) continue;
        Flight   *f = &sim->flights[i];
        StepData *s = &sim->last_step[i];
        double fuel_rem = f->current_mass - f->properties.empty_weight - f->payload;
        if (fuel_rem < 0.0) fuel_rem = 0.0;
        printf("%-8s %-7s %8.4f %9.4f %7.0f %8.1f %5.0f %6.2f %8.0f %9.4f %6.1f  %6.1f %5.3f %7.1f  %6.4f %6.4f  %8.0f %8.0f  %8.0f %8.0f  %7.2f  %6.0f %7.1f  %6.0f %7.0f\n",
               f->callsign, phase_name(s->phase),
               f->current_pos.latitude, f->current_pos.longitude, f->current_pos.altitude,
               s->dist_to_dest / 1000.0, s->cruise_alt,
               s->temperature, s->pressure, s->rho, s->speed_of_sound,
               s->ias_kts, s->mach, s->tas,
               s->cl, s->cd, s->lift, s->drag,
               s->thrust_per_engine, s->total_thrust, s->climb_rate,
               s->fuel_burned, s->horiz_dist / 1000.0,
               f->current_mass, fuel_rem);
    }
}

// ── Simulation threads ─────────────────────────────────────────────────────────

/**
 * @brief Dispatcher thread — posts step_go for each active child, then signals Results.
 *
 * @param arg Pointer to SimContext (cast from void *).
 * @return NULL
 */
static void *dispatcher_thread(void *arg) {
    SimContext *ctx = (SimContext *)arg;
    while (1) 
    {
        pthread_mutex_lock(&ctx->mutex);
        while (ctx->current_thread != THREAD_DISPATCHER && !ctx->sim_done)
            pthread_cond_wait(&ctx->cond_dispatch, &ctx->mutex);
        if (ctx->sim_done) { 
            pthread_mutex_unlock(&ctx->mutex); 
            break; 
        }
        
        bool active[MAX_FLIGHTS];
        memcpy(active, ctx->was_active, sizeof(active)); // copy was_active to active in order to unlock the mutex and post semaphores
        pthread_mutex_unlock(&ctx->mutex);

        for (int i = 0; i < ctx->num_flights; i++)
        {
            if (active[i])
            {
                sem_post(&ctx->shm->step_go[i]);
            }
        }

        pthread_mutex_lock(&ctx->mutex);
        ctx->current_thread = THREAD_RESULTS;
        pthread_cond_signal(&ctx->cond_results);
        pthread_mutex_unlock(&ctx->mutex);
    }
    return NULL;
}

/**
 * @brief Results thread — waits on step_done for each active child, then appends
 *        positions to history and signals Violations.
 *
 * @param arg Pointer to SimContext (cast from void *).
 * @return NULL
 */
static void *results_thread(void *arg) {
    SimContext *ctx = (SimContext *)arg;
    while (1) 
    {
        pthread_mutex_lock(&ctx->mutex);
        while (ctx->current_thread != THREAD_RESULTS && !ctx->sim_done)
        {
            pthread_cond_wait(&ctx->cond_results, &ctx->mutex);
        }
        if (ctx->sim_done) 
        {
            pthread_mutex_unlock(&ctx->mutex); 
            break; 
        }
        
        bool active[MAX_FLIGHTS];
        memcpy(active, ctx->was_active, sizeof(active));
        pthread_mutex_unlock(&ctx->mutex);

        for (int i = 0; i < ctx->num_flights; i++)
        {
            if (active[i])
            {
                sem_wait(&ctx->shm->step_done[i]); // block until child posts step_done
            }
        }

        for (int i = 0; i < ctx->num_flights; i++)
        {
            if (!active[i])
            {
                continue;
            }
            if (is_valid_position(&ctx->shm->sim.flights[i].current_pos))
            {
                append_position(&ctx->histories[i], &ctx->shm->sim.flights[i].current_pos);
            }
            else
            {
                printf("[WARN] Flight %d sent invalid position — update rejected\n",
                       ctx->shm->sim.flights[i].id);
            }
        }
        pthread_mutex_lock(&ctx->mutex);
        ctx->current_thread = THREAD_VIOLATIONS;
        pthread_cond_signal(&ctx->cond_violations);
        pthread_mutex_unlock(&ctx->mutex);
    }
    return NULL;
}

/**
 * @brief Violations thread — runs violation detection, advances the clock, 
 *        prints the step table, handles landing/leg-completion messages, then 
 *        signals main.
 *
 * @param arg Pointer to SimContext (cast from void *).
 * @return NULL
 */
static void *violations_thread(void *arg) {
    SimContext *ctx = (SimContext *)arg;
    SimulationData *sim = &ctx->shm->sim;

    while (1) 
    {
        pthread_mutex_lock(&ctx->mutex);
        while (ctx->current_thread != THREAD_VIOLATIONS && !ctx->sim_done) 
        {
            pthread_cond_wait(&ctx->cond_violations, &ctx->mutex);
        }
        if (ctx->sim_done) 
        {
            pthread_mutex_unlock(&ctx->mutex); 
            break; 
        }
        bool active[MAX_FLIGHTS];
        memcpy(active, ctx->was_active, sizeof(active));
        double step_time   = ctx->step_time;
        int    step_num    = ctx->step_num;
        int    total_steps = ctx->total_steps;
        pthread_mutex_unlock(&ctx->mutex);

        check_violations(sim, active, ctx->pids, ctx->num_flights);

        /* Remove collision-killed flights from active[] so the table and landing
           messages don't show them as if they completed normally. */
        for (int i = 0; i < ctx->num_flights; i++)
            if (active[i] && !sim->flights[i].is_active && sim->flights[i].violation_detected)
                active[i] = false;

        /* Advance the simulation clock now that all children have finished. */
        sim->current_time += sim->config.time_step;

        print_step_table(sim, ctx->prev_phase,
                         sim->current_time, step_num, total_steps,
                         step_time, active);

        /* Print landing summary for flights that completed normally this step. */
        for (int i = 0; i < ctx->num_flights; i++)
        {
            if (!active[i] || sim->flights[i].is_active)
            {
                continue;
            }
            Flight *f = &sim->flights[i];
            double remaining_fuel = f->current_mass - f->properties.empty_weight - f->payload;
            printf("[%s] Landed. Remaining fuel: %.0f kg | Total mass: %.0f kg\n",
                   f->callsign, remaining_fuel, f->current_mass);
            print_arrival_deviation(f->legs[f->current_leg].scheduled_arrival_offset_s,
                                    sim->current_time, sim->config.sim_start_seconds);
        }

        /* Print intermediate-stop messages for multi-leg flights.
           Child is blocked on step_go — safe to clear leg_just_completed here. */
        for (int i = 0; i < ctx->num_flights; i++) 
        {
            if (!active[i] || !sim->flights[i].leg_just_completed) 
            {
                continue;
            }
            sim->flights[i].leg_just_completed = false;
            Flight *f = &sim->flights[i];
            printf("[%s] Arrived at intermediate stop. Leg %d complete. "
                   "Next leg (leg %d) departs at offset %.0fs.\n",
                   f->callsign, f->current_leg - 1, f->current_leg, f->departure_offset_s);
            print_arrival_deviation(f->legs[f->current_leg - 1].scheduled_arrival_offset_s,
                                    sim->current_time, sim->config.sim_start_seconds);
        }

        /* Update phase history for transition detection in the next step. */
        for (int i = 0; i < ctx->num_flights; i++)
        {
            if (active[i])
            {
                ctx->prev_phase[i] = sim->last_step[i].phase;
            }
        }
        for (int i = 0; i < ctx->num_flights; i++)
        {
            if (active[i] && !sim->flights[i].is_active && sim->flights[i].leg_just_completed == false)
            {
                ctx->prev_phase[i] = PHASE_GATE;
            }
        }

        fflush(stdout);

        /* Save current positions as prev_pos for next step's continuous collision detection. */
        for (int i = 0; i < ctx->num_flights; i++)
            if (sim->flights[i].is_active)
                sim->prev_pos[i] = sim->flights[i].current_pos;

        pthread_mutex_lock(&ctx->mutex);
        ctx->current_thread = THREAD_IDLE;
        pthread_cond_signal(&ctx->cond_step_done);
        pthread_mutex_unlock(&ctx->mutex);
    }
    return NULL;
}

// ── Report finalisation ────────────────────────────────────────────────────────

/**
 * @brief Populates the SimulationReport with final per-flight data, then forks
 *        report_generator and sends it SimulationConfig + SimulationReport via a pipe.
 *
 * report_generator reads the two structs from its stdin and produces the JSON file.
 *
 * @param sim          Pointer to the local simulation data block.
 * @param sim_end_time Final simulation clock value in seconds.
 */
static void finalize_and_write_report(SimulationData *sim, double sim_end_time, pid_t *pids) {
    sim->report.sim_end_time = sim_end_time;

    int count = 0;
    for (int i = 0; i < sim->num_flights; i++) {
        if (pids[i] <= 0) continue;
        Flight       *f      = &sim->flights[i];
        FlightStatus *status = &sim->report.flight_statuses[count++];

        strncpy(status->callsign, f->callsign, CALLSIGN_LEN - 1);
        status->callsign[CALLSIGN_LEN - 1] = '\0';
        status->completed_successfully = !f->is_active &&
                                          f->departure_offset_s < sim->config.end_time;
        status->had_violation      = f->violation_detected;
        double fuel_rem = f->current_mass - f->properties.empty_weight - f->payload;
        status->fuel_remaining     = (fuel_rem > 0) ? fuel_rem : 0.0;
        status->num_legs_completed = f->current_leg;
    }
    sim->report.total_flights = count;
    sim->report.total_violations_recorded = sim->total_violations_detected;

    int report_pipe[2];
    if (pipe(report_pipe) < 0) {
        perror("pipe for report_generator failed");
        return;
    }

    pid_t rpid = fork();
    if (rpid < 0) {
        perror("fork for report_generator failed");
        close(report_pipe[0]);
        close(report_pipe[1]);
        return;
    }

    if (rpid == 0) {
        // Child: pass the pipe read-end fd as argv[1] — inherited across execl.
        close(report_pipe[1]);
        char fd_str[16];
        snprintf(fd_str, sizeof(fd_str), "%d", report_pipe[0]);
        execl("./report_generator", "report_generator", fd_str, NULL);
        perror("execl report_generator failed");
        exit(EXIT_FAILURE);
    }

    // Parent: write config + report into the pipe in chunks, then wait.
    close(report_pipe[0]);
    const char *p;
    size_t remaining;
    p = (const char *)&sim->config; remaining = sizeof(sim->config);
    while (remaining > 0) { ssize_t w = write(report_pipe[1], p, remaining); if (w <= 0) break; p += w; remaining -= (size_t)w; }
    p = (const char *)&sim->report; remaining = sizeof(sim->report);
    while (remaining > 0) { ssize_t w = write(report_pipe[1], p, remaining); if (w <= 0) break; p += w; remaining -= (size_t)w; }
    close(report_pipe[1]);

    waitpid(rpid, NULL, 0);
}

// ── Cleanup ────────────────────────────────────────────────────────────────────

static void cleanup(SharedBlock *shm, pid_t *pids, int n) {
    shm->terminate = true;
    signal(SIGPIPE, SIG_IGN);

    /* Unblock children that are waiting on step_go. */
    for (int i = 0; i < n; i++)
        if (pids[i] > 0)
            sem_post(&shm->step_go[i]);

    for (int i = 0; i < n; i++)
        if (pids[i] > 0)
            waitpid(pids[i], NULL, 0);
}

// ── Flight selection menu ──────────────────────────────────────────────────────

/**
 * @brief Displays an interactive menu letting the user choose which flights to simulate.
 *
 * Out-of-scope flights (departure outside the sim window) are shown as non-selectable
 * with an explanation. In-scope flights start included and can be toggled by number.
 * When the user presses Enter with no input, the selection is confirmed.
 *
 * @param sim Pointer to the local simulation data block (modifies flights[i].is_active).
 * @return Number of flights that will be forked.
 */
static int run_flight_selection_menu(SimulationData *sim) {
    bool in_scope[MAX_FLIGHTS];
    bool included[MAX_FLIGHTS];

    int sim_start_h = (int)(sim->config.sim_start_seconds / 3600);
    int sim_start_m = (int)(fmod(sim->config.sim_start_seconds, 3600.0) / 60.0);
    int sim_end_s   = (int)(sim->config.sim_start_seconds + sim->config.end_time);

    for (int i = 0; i < sim->num_flights; i++) {
        Flight *f = &sim->flights[i];
        in_scope[i] = (f->departure_offset_s >= 0.0 &&
                       f->departure_offset_s  < sim->config.end_time);
        included[i] = false;
    }

    while (1) {
        printf("\n╔══════════════════════════════════════════════════════════════╗\n");
        printf("║                   FLIGHT SELECTION MENU                      ║\n");
        printf("╚══════════════════════════════════════════════════════════════╝\n");
        printf("  Sim window: %02d:%02d → %02d:%02d\n\n",
               sim_start_h, sim_start_m,
               sim_end_s / 3600, (sim_end_s % 3600) / 60);

        for (int i = 0; i < sim->num_flights; i++) {
            Flight *f = &sim->flights[i];
            double dep_wall = sim->config.sim_start_seconds + f->departure_offset_s;
            int dh = ((int)dep_wall / 3600) % 24;
            int dm = ((int)dep_wall % 3600) / 60;

            if (!in_scope[i]) {
                if (f->departure_offset_s < 0.0) {
                    double actual = sim->config.sim_start_seconds + f->departure_offset_s;
                    int ah = ((int)actual / 3600) % 24;
                    int am = ((int)actual % 3600) / 60;
                    printf("  [OUT OF SCOPE] %2d. %-10s  departs %02d:%02d"
                           " — before simulation window start (%02d:%02d)\n",
                           i + 1, f->callsign, ah, am, sim_start_h, sim_start_m);
                } else {
                    printf("  [OUT OF SCOPE] %2d. %-10s  departs %02d:%02d"
                           " — after simulation window end (%02d:%02d)\n",
                           i + 1, f->callsign, dh, dm,
                           sim_end_s / 3600, (sim_end_s % 3600) / 60);
                }
            } else {
                printf("  [%s] %2d. %-10s  departs %02d:%02d\n",
                       included[i] ? "SELECTED  " : "          ",
                       i + 1, f->callsign, dh, dm);
            }
        }

        printf("\n  Enter a flight number to add/remove, 'a' to select all, or press Enter to start: ");
        fflush(stdout);

        char line[64];
        if (!fgets(line, sizeof(line), stdin) || line[0] == '\n') {
            int selected = 0;
            for (int i = 0; i < sim->num_flights; i++)
                if (included[i]) selected++;
            if (selected == 0) {
                printf("  No flights selected. Please select at least one.\n");
                continue;
            }
            break;
        }

        if (line[0] == 'a' || line[0] == 'A') {
            for (int i = 0; i < sim->num_flights; i++)
                if (in_scope[i]) included[i] = true;
            continue;
        }

        int num = atoi(line);
        if (num >= 1 && num <= sim->num_flights) {
            if (!in_scope[num - 1])
                printf("  Flight %d is outside the simulation window and cannot be selected.\n", num);
            else
                included[num - 1] = !included[num - 1];
        } else {
            printf("  Invalid number. Enter a value between 1 and %d.\n", sim->num_flights);
        }
    }

    int count = 0;
    for (int i = 0; i < sim->num_flights; i++) {
        sim->flights[i].is_active = included[i];
        if (included[i]) count++;
    }
    return count;
}

// ── Main ───────────────────────────────────────────────────────────────────────

int main(void) {
    printf("=== AISafe Simulation Engine starting ===\n");

    /* Clean up any leftover shared memory from a previous crashed run. */
    shm_unlink("/aisafe_sim");

    int shm_fd = shm_open("/aisafe_sim", O_CREAT | O_RDWR, 0600);
    if (shm_fd < 0) { perror("shm_open"); return EXIT_FAILURE; }
    if (ftruncate(shm_fd, sizeof(SharedBlock)) < 0) { perror("ftruncate"); return EXIT_FAILURE; }

    SharedBlock *shm = mmap(NULL, sizeof(SharedBlock),
                            PROT_READ | PROT_WRITE, MAP_SHARED, shm_fd, 0);
    close(shm_fd);
    if (shm == MAP_FAILED) { perror("mmap"); return EXIT_FAILURE; }

    memset(shm, 0, sizeof(SharedBlock));

    for (int i = 0; i < MAX_FLIGHTS; i++) {
        sem_init(&shm->step_go[i],   1, 0);
        sem_init(&shm->step_done[i], 1, 0);
    }

    SimulationData *sim = &shm->sim;

    if (!load_simulation_settings("simulation_settings.json", &sim->config)) {
        fprintf(stderr, "[SIM] Aborting: simulation settings are invalid.\n");
        return EXIT_FAILURE;
    }

    if (!load_all_flight_plans("flightplansSim", sim)) {
        fprintf(stderr, "[SIM] Aborting: one or more flight plans failed validation.\n");
        return EXIT_FAILURE;
    }
    printf("Loaded %d flight(s). Simulation: %.0f s -> %.0f s  (step %.0f s = %.0f min)\n",
           sim->num_flights,
           sim->config.start_time, sim->config.end_time,
           sim->config.time_step, sim->config.time_step / 60.0);

    sim->report.sim_start_time            = sim->config.start_time;
    sim->report.total_flights             = sim->num_flights;
    sim->report.total_violations_recorded = 0;
    sim->report.report_ready              = false;
    time_t wall_start                     = time(NULL);
    sim->report.wall_clock_start          = (double)wall_start;

    int num_forked = run_flight_selection_menu(sim);

    printf("\n%d flight(s) selected — forking %d child processes.\n",
           num_forked, num_forked);

    int total_steps = (int)((sim->config.end_time - sim->config.start_time) / sim->config.time_step);

    /* ── Fork one child per selected flight ─────────────────────────────── */
    pid_t pids[MAX_FLIGHTS];
    memset(pids, 0, sizeof(pids));
    sim->current_time = sim->config.start_time;

    for (int i = 0; i < sim->num_flights; i++) {
        sim->flights[i].process_id = 0;
        if (!sim->flights[i].is_active) continue;

        fflush(stdout);
        pid_t pid = fork();
        if (pid < 0) {
            perror("fork failed");
            cleanup(shm, pids, sim->num_flights);
            return EXIT_FAILURE;
        }

        if (pid == 0) {
            run_flight_process(shm, i);
            exit(EXIT_SUCCESS);
        }

        pids[i] = pid;
        sim->flights[i].process_id = pid;
        printf("Forked child PID %d for flight %s (departs at offset %.0fs)\n",
               pid, sim->flights[i].callsign, sim->flights[i].departure_offset_s);
    }

    /* ── Startup barrier: wait for all children to post step_done once ───── */
    for (int i = 0; i < sim->num_flights; i++)
        if (pids[i] > 0)
            sem_wait(&shm->step_done[i]);

    /* Initialise prev_pos to each flight's departure position. */
    for (int i = 0; i < sim->num_flights; i++)
        if (pids[i] > 0)
            sim->prev_pos[i] = sim->flights[i].current_pos;

    /* ── Initialise SimContext ─────────────────────────────────────────── */
    static PositionHistory histories[MAX_FLIGHTS];
    for (int i = 0; i < MAX_FLIGHTS; i++) { histories[i].count = 0; histories[i].next = 0; }

    for (int i = 0; i < sim->num_flights; i++) {
        if (pids[i] <= 0) continue;
        Position dep = sim->flights[i].flight_plan.waypoints[0].pos;
        dep.timestamp = 0.0;
        append_position(&histories[i], &dep);
    }

    SimContext ctx;
    memset(&ctx, 0, sizeof(ctx));
    ctx.shm         = shm;
    ctx.num_flights = sim->num_flights;
    ctx.total_steps = total_steps;
    ctx.histories   = histories;
    ctx.sim_done    = false;
    memcpy(ctx.pids, pids, sizeof(pids));
    pthread_mutex_init(&ctx.mutex, NULL);
    pthread_cond_init(&ctx.cond_dispatch,   NULL);
    pthread_cond_init(&ctx.cond_results,    NULL);
    pthread_cond_init(&ctx.cond_violations, NULL);
    pthread_cond_init(&ctx.cond_step_done,  NULL);
    ctx.current_thread = THREAD_IDLE;

    for (int i = 0; i < MAX_FLIGHTS; i++)
        ctx.prev_phase[i] = PHASE_GATE;

    pthread_t thr_dispatcher, thr_results, thr_violations;
    pthread_create(&thr_dispatcher,  NULL, dispatcher_thread,  &ctx);
    pthread_create(&thr_results,     NULL, results_thread,     &ctx);
    pthread_create(&thr_violations,  NULL, violations_thread,  &ctx);

    /* ── Step 0: print initial state ─────────────────────────────────────── */
    printf("\n--- Simulation running ---\n");
    {
        int wall_s = (int)sim->config.sim_start_seconds;
        struct tm wall_tm0 = {0};
        wall_tm0.tm_year = sim->config.sim_start_year - 1900;
        wall_tm0.tm_mon  = sim->config.sim_start_month - 1;
        wall_tm0.tm_mday = sim->config.sim_start_day;
        wall_tm0.tm_hour = wall_s / 3600;
        wall_tm0.tm_min  = (wall_s % 3600) / 60;
        mktime(&wall_tm0);
        printf("\n╔══════════════════════════════════════════════════════════════════════"
               "════════════════════════════════════════════════════════════════════╗\n");
        printf("║  t=       0 s  [%02d/%02d/%04d %02d:%02d]  |  step   0 / %-3d  |  active= 0  pending=%2d  |  violations=  0"
               "                              ║\n",
               wall_tm0.tm_mday, wall_tm0.tm_mon + 1, wall_tm0.tm_year + 1900,
               wall_tm0.tm_hour, wall_tm0.tm_min,
               (int)((sim->config.end_time - sim->config.start_time) / sim->config.time_step),
               num_forked);
        printf("╚══════════════════════════════════════════════════════════════════════"
               "════════════════════════════════════════════════════════════════════╝\n\n");
        printf("%-8s %-7s %8s %9s %7s %8s %5s %6s %8s %9s %6s  %6s %5s %7s  %6s %6s  %8s %8s  %8s %8s  %7s  %6s %7s  %6s %6s\n",
               "CALLSGN","PHASE","LAT(deg)","LON(deg)","ALT(m)","DEST(km)","CA(m)",
               "T(K)","P(Pa)","rho","a(m/s)","IAS(kt)","Mach","TAS(m/s)",
               "CL","CD","Lift(N)","Drag(N)","Te(N)","Tt(N)","VS(m/s)",
               "dF(kg)","dD(km)","M(kg)","Frem(kg)");
        printf("%-8s %-7s %8s %9s %7s %8s %5s %6s %8s %9s %6s  %6s %5s %7s  %6s %6s  %8s %8s  %8s %8s  %7s  %6s %7s  %6s %6s\n",
               "--------","-------","--------","---------","-------","--------","-----",
               "------","--------","---------","------","------","-----","-------",
               "------","------","--------","--------","--------","--------","-------",
               "------","-------","------","-------");
        for (int i = 0; i < sim->num_flights; i++) {
            Flight *f = &sim->flights[i];
            if (!f->is_active) continue;
            double fuel_rem = f->initial_fuel_mass;
            int last_wp = f->legs[0].waypoint_end_index;
            double dest_lat = f->flight_plan.waypoints[last_wp].pos.latitude;
            double dest_lon = f->flight_plan.waypoints[last_wp].pos.longitude;
            const double R = 6371000.0;
            double lat1 = f->current_pos.latitude  * M_PI / 180.0;
            double lat2 = dest_lat * M_PI / 180.0;
            double dlat = (dest_lat - f->current_pos.latitude)  * M_PI / 180.0;
            double dlon = (dest_lon - f->current_pos.longitude) * M_PI / 180.0;
            double a = sin(dlat/2)*sin(dlat/2) + cos(lat1)*cos(lat2)*sin(dlon/2)*sin(dlon/2);
            double dist_to_dest = R * 2.0 * atan2(sqrt(a), sqrt(1.0 - a));
            double cruise_alt = f->flight_plan.waypoints[f->legs[0].waypoint_start_index].pos.altitude;
            printf("%-8s %-7s %8.4f %9.4f %7.0f %8.1f %5.0f %6s %8s %9s %6s  %6s %5s %7s  %6s %6s  %8s %8s  %8s %8s  %7s  %6s %7s  %6.0f %7.0f\n",
                   f->callsign, "GATE   ",
                   f->current_pos.latitude, f->current_pos.longitude,
                   f->current_pos.altitude, dist_to_dest / 1000.0, cruise_alt,
                   "-","-","-","-","-","-","-","-","-","-","-","-","-","-","-","-",
                   f->initial_mass, fuel_rem);
        }
    }

    /* ── Main simulation loop ────────────────────────────────────────────── */
    int step_num = 0;

    while (sim->current_time < sim->config.end_time) {

        double step_time = sim->current_time;
        int active_count = 0;
        bool was_active[MAX_FLIGHTS];
        for (int i = 0; i < sim->num_flights; i++) {
            was_active[i] = sim->flights[i].is_active &&
                            step_time >= sim->flights[i].departure_offset_s;
            if (was_active[i]) active_count++;
        }

        if (active_count == 0) {
            bool any_pending = false;
            for (int i = 0; i < sim->num_flights; i++)
                if (sim->flights[i].is_active) { any_pending = true; break; }
            if (!any_pending) break;
            sim->current_time += sim->config.time_step;
            continue;
        }

        step_num++;

        pthread_mutex_lock(&ctx.mutex);
        memcpy(ctx.was_active, was_active, sizeof(was_active));
        ctx.step_time      = step_time;
        ctx.step_num       = step_num;
        ctx.current_thread = THREAD_DISPATCHER;
        pthread_cond_signal(&ctx.cond_dispatch);
        pthread_cond_wait(&ctx.cond_step_done, &ctx.mutex);
        pthread_mutex_unlock(&ctx.mutex);

        if (sim->termination_requested) {
            printf("[SIM] Simulation terminated early — max violations exceeded.\n");
            break;
        }
    }

    /* ── Shutdown threads ────────────────────────────────────────────────── */
    pthread_mutex_lock(&ctx.mutex);
    ctx.sim_done = true;
    pthread_cond_broadcast(&ctx.cond_dispatch);
    pthread_cond_broadcast(&ctx.cond_results);
    pthread_cond_broadcast(&ctx.cond_violations);
    pthread_mutex_unlock(&ctx.mutex);

    pthread_join(thr_dispatcher,  NULL);
    pthread_join(thr_results,     NULL);
    pthread_join(thr_violations,  NULL);

    pthread_mutex_destroy(&ctx.mutex);
    pthread_cond_destroy(&ctx.cond_dispatch);
    pthread_cond_destroy(&ctx.cond_results);
    pthread_cond_destroy(&ctx.cond_violations);
    pthread_cond_destroy(&ctx.cond_step_done);

    /* ── Cleanup children ────────────────────────────────────────────────── */
    cleanup(shm, pids, sim->num_flights);

    finalize_and_write_report(sim, sim->current_time, pids);

    printf("\n╔══════════════════════════════════════════════════════╗\n");
    printf("║              SIMULATION COMPLETE                     ║\n");
    printf("╚══════════════════════════════════════════════════════╝\n");
    printf("Total violations detected: %d\n", sim->total_violations_detected);
    for (int i = 0; i < sim->num_flights; i++) {
        if (pids[i] <= 0) continue;
        printf("  %-8s : %s\n", sim->flights[i].callsign,
               sim->flights[i].violation_detected ? "HAD VIOLATION" : "clean");
    }

    printf("\n╔══════════════════════════════════════════════════════╗\n");
    printf("║           POSITION HISTORY                           ║\n");
    printf("╚══════════════════════════════════════════════════════╝\n");
    for (int i = 0; i < sim->num_flights; i++) {
        if (pids[i] <= 0 || histories[i].count == 0) continue;
        printf("\n  %s  (%d steps)\n", sim->flights[i].callsign, histories[i].count);
        printf("  %6s  %8s  %9s  %9s\n", "STEP", "LAT", "LON", "ALT (m)");
        printf("  %6s  %8s  %9s  %9s\n", "------", "--------", "---------", "---------");
        int start = (histories[i].count < MAX_HISTORY) ? 0 : histories[i].next;
        for (int j = 0; j < histories[i].count; j++) {
            Position *p = &histories[i].entries[(start + j) % MAX_HISTORY];
            printf("  %6d  %8.4f  %9.4f  %9.0f\n",
                   j, p->latitude, p->longitude, p->altitude);
        }
    }
    printf("\n");

    /* ── Release shared memory ───────────────────────────────────────────── */
    for (int i = 0; i < MAX_FLIGHTS; i++) {
        sem_destroy(&shm->step_go[i]);
        sem_destroy(&shm->step_done[i]);
    }
    munmap(shm, sizeof(SharedBlock));
    shm_unlink("/aisafe_sim");

    return EXIT_SUCCESS;
}
