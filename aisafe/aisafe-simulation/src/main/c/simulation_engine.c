#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>
#include <semaphore.h>
#include "structs.h"
#include "json_parser.h"
#include "flight_simulation.h"
#include "position_tracker.h"
#include <signal.h>

// Leading '/' is required by POSIX for shm_open names.
#define SHM_NAME "/aisafe_sim"

/**
 * @brief Creates and maps the POSIX shared memory block used by all processes.
 *
 * Removes any leftover object from a previous run, creates a new one,
 * sizes it to hold SharedMemoryData, maps it MAP_SHARED, and zero-initialises it.
 *
 * @return Pointer to the mapped SharedMemoryData block. Exits on failure.
 */
static SharedMemoryData *setup_shared_memory(void) {
    shm_unlink(SHM_NAME);

    int fd = shm_open(SHM_NAME, O_CREAT | O_RDWR, 0666);
    if (fd < 0) {
        perror("shm_open failed");
        exit(EXIT_FAILURE);
    }

    if (ftruncate(fd, sizeof(SharedMemoryData)) < 0) {
        perror("ftruncate failed");
        exit(EXIT_FAILURE);
    }

    SharedMemoryData *shm = mmap(NULL, sizeof(SharedMemoryData),
                                  PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    close(fd);

    if (shm == MAP_FAILED) {
        perror("mmap failed");
        exit(EXIT_FAILURE);
    }

    memset(shm, 0, sizeof(SharedMemoryData));
    return shm;
}

/**
 * @brief Creates all named POSIX semaphores needed for the step-lock pattern.
 *
 * Creates n per-flight step semaphores (initialised to 0, so children block
 * until the parent posts) and one shared main semaphore for children to signal
 * completion. Names include the parent PID to avoid collisions between concurrent runs.
 *
 * @param pid          Parent process ID, used to build unique semaphore names.
 * @param n            Number of step semaphores to create (one per flight).
 * @param step_sems    Output array of n sem_t* pointers.
 * @param main_sem_out Output pointer to the shared main semaphore.
 */
static void init_semaphores(pid_t pid, int n, sem_t **step_sems, sem_t **main_sem_out) {
    for (int i = 0; i < n; i++) {
        char name[48];
        snprintf(name, sizeof(name), "/aisafe_step_%d_%d", (int)pid, i);
        sem_unlink(name);
        // Initial value 0: child blocks until parent posts.
        step_sems[i] = sem_open(name, O_CREAT, 0666, 0);
        if (step_sems[i] == SEM_FAILED) {
            perror("sem_open step");
            exit(EXIT_FAILURE);
        }
    }

    char main_name[48];
    snprintf(main_name, sizeof(main_name), "/aisafe_main_%d", (int)pid);
    sem_unlink(main_name);
    *main_sem_out = sem_open(main_name, O_CREAT, 0666, 0);
    if (*main_sem_out == SEM_FAILED) {
        perror("sem_open main");
        exit(EXIT_FAILURE);
    }
}

/**
 * @brief Checks all active flight pairs for proximity violations and all flights for fuel exhaustion.
 *
 * Sends SIGUSR1 to each aircraft involved in a proximity violation.
 * If total violations exceed config.max_violations_threshold, sends SIGTERM to all
 * active flights and sets shm->termination_requested.
 *
 * @param shm        Pointer to shared memory (reads positions, writes violation counts/flags).
 * @param was_active Boolean array indicating which flights were active at the start of this step.
 * @param pids       Array of child PIDs indexed by flight slot.
 * @param num_flights Total number of flight slots.
 */
static void check_violations(SharedMemoryData *shm, bool *was_active, pid_t *pids, int num_flights) {
    const double R = 6371000.0;

    for (int i = 0; i < shm->num_flights; i++) {
        if (!was_active[i] || !shm->flights[i].is_active) continue;

        for (int j = i + 1; j < shm->num_flights; j++) {
            if (!was_active[j] || !shm->flights[j].is_active) continue;

            Position *a = &shm->flights[i].current_pos;
            Position *b = &shm->flights[j].current_pos;

            double lat_avg = (a->latitude + b->latitude) / 2.0 * M_PI / 180.0;
            double dx = (b->longitude - a->longitude) * M_PI / 180.0 * cos(lat_avg) * R;
            double dy = (b->latitude  - a->latitude)  * M_PI / 180.0 * R;
            double dz = b->altitude - a->altitude;
            double dist = sqrt(dx*dx + dy*dy + dz*dz);

            if (dist < shm->config.safety_distance_threshold) {
                shm->total_violations_detected++;
                shm->flights[i].violation_detected = true;
                shm->flights[j].violation_detected = true;
                printf("  *** VIOLATION: %s <-> %s  dist=%.0f m  (threshold %.0f m) ***\n",
                       shm->flights[i].callsign,
                       shm->flights[j].callsign,
                       dist,
                       shm->config.safety_distance_threshold);

                if (shm->report.total_violations_recorded < MAX_RECORDED_VIOLATIONS) {
                    ViolationEvent *evt = &shm->report.violations[shm->report.total_violations_recorded];
                    evt->timestamp = shm->current_time;
                    evt->flight_a_id = i;
                    evt->flight_b_id = j;
                    evt->pos_a = shm->flights[i].current_pos;
                    evt->pos_b = shm->flights[j].current_pos;
                    evt->vel_a = shm->flights[i].velocity;
                    evt->vel_b = shm->flights[j].velocity;
                    evt->distance = dist;
                    shm->report.total_violations_recorded++;
                }

                if (pids[i] > 0) kill(pids[i], SIGUSR1);
                if (pids[j] > 0) kill(pids[j], SIGUSR1);
            }
        }
    }

    for (int i = 0; i < shm->num_flights; i++) {
        if (!was_active[i]) continue;
        Flight *f = &shm->flights[i];
        double fuel_rem = f->current_mass - f->properties.empty_weight - f->payload;
        if (fuel_rem <= 0.0 && !f->fuel_exhaustion_reported) {
            f->fuel_exhaustion_reported = true;
            f->violation_detected = true;
            shm->total_violations_detected++;
            printf("  *** VIOLATION: %s fuel exhausted (0 kg remaining) ***\n",
                   f->callsign);
        }
    }

    if (shm->config.max_violations_threshold > 0 &&
        shm->total_violations_detected > shm->config.max_violations_threshold) {
        printf("  [SIM] Violations (%d) exceeded threshold (%d) — "
               "sending SIGTERM to all active flights.\n",
               shm->total_violations_detected,
               shm->config.max_violations_threshold);
        for (int i = 0; i < num_flights; i++) {
            if (pids[i] > 0 && shm->flights[i].is_active)
                kill(pids[i], SIGTERM);
        }
        shm->termination_requested = true;
    }
}

/**
 * @brief Converts a FlightPhase enum value to a fixed-width display string.
 *
 * @param p Flight phase.
 * @return 7-character null-terminated string (e.g. "CLIMB  ").
 */
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
 * @brief Prints the scheduled vs. actual arrival time and deviation for one flight leg.
 *
 * Does nothing if scheduled_offset_s is negative (arrival not specified in the flight plan).
 *
 * @param scheduled_offset_s Scheduled arrival as seconds offset from sim start, or negative if unset.
 * @param actual_offset_s    Actual arrival as seconds offset from sim start.
 * @param sim_start_s        Sim start time as seconds since midnight (wall-clock anchor).
 */
static void print_arrival_deviation(double scheduled_offset_s,
                                    double actual_offset_s,
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
 * Emits a step banner with time/step/active/violation counters, phase transition
 * messages for any flights that changed phase, a column header, and one data row
 * per flight that was active at the start of this step.
 *
 * @param shm        Pointer to shared memory (reads flight positions and violation count).
 * @param msgs       Array of PipeMessage structs, one per flight slot, from the current step.
 * @param prev_ph    Previous phase per flight (used to detect phase transitions).
 * @param sim_time   Current simulation clock value in seconds (after advancing).
 * @param step_num   Current step number (1-based).
 * @param total_steps Total number of steps in the simulation.
 * @param step_time  Simulation clock value at the start of this step (before advancing).
 * @param was_active Boolean array indicating which flights were active at step start.
 */
static void print_step_table(SharedMemoryData *shm,
                              PipeMessage *msgs,
                              FlightPhase *prev_ph,
                              double sim_time,
                              int step_num, int total_steps,
                              double step_time,
                              bool *was_active) {

    int active = 0;
    for (int i = 0; i < shm->num_flights; i++)
        if (shm->flights[i].is_active && step_time >= shm->flights[i].departure_offset_s)
            active++;
    int pending = 0;
    for (int i = 0; i < shm->num_flights; i++)
        if (shm->flights[i].is_active && step_time < shm->flights[i].departure_offset_s)
            pending++;

    int wall_s = (int)(shm->config.sim_start_seconds + sim_time);
    printf("\n");
    printf("╔══════════════════════════════════════════════════════════════════════"
           "════════════════════════════════════════════════════════════════════╗\n");
    printf("║  t=%7.0f s  [%02d:%02d]  |  step %3d / %-3d  |  active=%2d  pending=%2d  |  violations=%3d"
           "                                                      ║\n",
           sim_time, wall_s / 3600, (wall_s % 3600) / 60,
           step_num, total_steps,
           active, pending, shm->total_violations_detected);
    printf("╚══════════════════════════════════════════════════════════════════════"
           "════════════════════════════════════════════════════════════════════╝\n");

    for (int i = 0; i < shm->num_flights; i++) {
        if (!was_active[i]) continue;
        FlightPhase cur = msgs[i].step.phase;
        if (cur != prev_ph[i]) {
            printf("  >>> %s: %s --> %s <<<\n",
                   shm->flights[i].callsign, phase_name(prev_ph[i]), phase_name(cur));
        }
    }

    printf("\n");
    printf("%-8s %-7s %8s %9s %7s %8s %5s %6s %8s %9s %6s  %6s %5s %7s  %6s %6s  %8s %8s  %8s %8s  %7s  %6s %7s  %6s %6s\n",
           "CALLSGN","PHASE","LAT(deg)","LON(deg)","ALT(m)","DEST(km)","CA(m)",
           "T(K)","P(Pa)","rho","a(m/s)",
           "IAS(kt)","Mach","TAS(m/s)",
           "CL","CD","Lift(N)","Drag(N)",
           "Te(N)","Tt(N)","VS(m/s)",
           "dF(kg)","dD(km)","M(kg)","Frem(kg)");

    printf("%-8s %-7s %8s %9s %7s %8s %5s %6s %8s %9s %6s  %6s %5s %7s  %6s %6s  %8s %8s  %8s %8s  %7s  %6s %7s  %6s %6s\n",
           "--------","-------","--------","---------","-------","--------","-----",
           "------","--------","---------","------",
           "------","-----","-------",
           "------","------","--------","--------",
           "--------","--------","-------",
           "------","-------","------","-------");

    for (int i = 0; i < shm->num_flights; i++) {
        if (!was_active[i]) continue;

        Flight   *f  = &shm->flights[i];
        StepData *s  = &msgs[i].step;

        double fuel_rem = f->current_mass - f->properties.empty_weight - f->payload;
        if (fuel_rem < 0.0) fuel_rem = 0.0;

        printf("%-8s %-7s %8.4f %9.4f %7.0f %8.1f %5.0f %6.2f %8.0f %9.4f %6.1f  %6.1f %5.3f %7.1f  %6.4f %6.4f  %8.0f %8.0f  %8.0f %8.0f  %7.2f  %6.0f %7.1f  %6.0f %7.0f\n",
               f->callsign,
               phase_name(s->phase),
               f->current_pos.latitude,
               f->current_pos.longitude,
               f->current_pos.altitude,
               s->dist_to_dest / 1000.0,
               s->cruise_alt,
               s->temperature,
               s->pressure,
               s->rho,
               s->speed_of_sound,
               s->ias_kts,
               s->mach,
               s->tas,
               s->cl,
               s->cd,
               s->lift,
               s->drag,
               s->thrust_per_engine,
               s->total_thrust,
               s->climb_rate,
               s->fuel_burned,
               s->horiz_dist / 1000.0,
               f->current_mass,
               fuel_rem);
    }
}

/**
 * @brief Populates the SimulationReport in shared memory with final per-flight data.
 *
 * Records completion status, violation flags, fuel remaining, and legs completed
 * for each flight, then sets report_ready = true so the report generator can proceed.
 *
 * @param shm          Pointer to shared memory (reads flights[], writes report).
 * @param sim_end_time Final simulation clock value in seconds.
 */
static void finalize_simulation_report(SharedMemoryData *shm, double sim_end_time) {
    shm->report.sim_end_time = sim_end_time;

    for (int i = 0; i < shm->num_flights; i++) {
        Flight *f = &shm->flights[i];
        FlightStatus *status = &shm->report.flight_statuses[i];

        strncpy(status->callsign, f->callsign, CALLSIGN_LEN - 1);
        status->callsign[CALLSIGN_LEN - 1] = '\0';

        status->completed_successfully = !f->is_active &&
                                         f->departure_offset_s < shm->config.end_time;
        status->had_violation = f->violation_detected;

        double fuel_rem = f->current_mass - f->properties.empty_weight - f->payload;
        status->fuel_remaining = (fuel_rem > 0) ? fuel_rem : 0.0;

        status->num_legs_completed = f->current_leg;
    }

    shm->report.report_ready = true;
}

/**
 * @brief Releases all IPC resources and waits for child processes to exit.
 *
 * Sets termination_requested, unblocks any children waiting on step semaphores,
 * reaps all child processes, then closes and unlinks every semaphore and unmaps
 * shared memory. Does NOT unlink the shared memory object — the report generator
 * is responsible for that after reading the report.
 *
 * @param shm        Pointer to the shared memory block.
 * @param pids       Array of child PIDs indexed by flight slot.
 * @param n          Number of flights (length of pids and step_sems).
 * @param parent_pid Parent PID used to reconstruct semaphore names for unlinking.
 * @param step_sems  Array of per-flight step semaphore pointers.
 * @param main_sem   Shared main semaphore pointer.
 */
static void cleanup(SharedMemoryData *shm, pid_t *pids, int n,
                    pid_t parent_pid, sem_t **step_sems, sem_t *main_sem) {

    shm->termination_requested = true;

    // Unblock any child blocked on sem_wait so waitpid() doesn't hang.
    for (int i = 0; i < n; i++)
        sem_post(step_sems[i]);

    for (int i = 0; i < n; i++)
        if (pids[i] > 0) waitpid(pids[i], NULL, 0);

    for (int i = 0; i < n; i++) {
        sem_close(step_sems[i]);
        char name[48];
        snprintf(name, sizeof(name), "/aisafe_step_%d_%d", (int)parent_pid, i);
        sem_unlink(name);
    }

    char main_name[48];
    snprintf(main_name, sizeof(main_name), "/aisafe_main_%d", (int)parent_pid);
    sem_close(main_sem);
    sem_unlink(main_name);

    munmap(shm, sizeof(SharedMemoryData));

    // Do not shm_unlink here: the report generator attaches after simulation ends
    // and is responsible for removing the shared memory object.
}

int main() {

    printf("=== AISafe Simulation Engine starting ===\n");

    SharedMemoryData *shm = setup_shared_memory();

    if (!load_simulation_settings("simulation_settings.json", &shm->config)) {
        fprintf(stderr, "[SIM] Aborting: simulation settings are invalid.\n");
        munmap(shm, sizeof(SharedMemoryData));
        shm_unlink("/aisafe_sim");
        return EXIT_FAILURE;
    }

    if (!load_all_flight_plans("flightplansSim", shm)) {
        fprintf(stderr, "[SIM] Aborting: one or more flight plans failed validation.\n");
        munmap(shm, sizeof(SharedMemoryData));
        shm_unlink("/aisafe_sim");
        return EXIT_FAILURE;
    }
    printf("Loaded %d flight(s). Simulation: %.0f s -> %.0f s  (step %.0f s = %.0f min)\n",
           shm->num_flights,
           shm->config.start_time, shm->config.end_time,
           shm->config.time_step, shm->config.time_step / 60.0);

    shm->report.sim_start_time = shm->config.start_time;
    shm->report.total_flights = shm->num_flights;
    shm->report.total_violations_recorded = 0;
    shm->report.report_ready = false;
    time_t wall_start = time(NULL);
    shm->report.wall_clock_start = (double)wall_start;

    int num_forked = 0;
    int sim_start_h = (int)(shm->config.sim_start_seconds / 3600);
    int sim_start_m = (int)(fmod(shm->config.sim_start_seconds, 3600.0) / 60.0);
    int sim_end_s   = (int)(shm->config.sim_start_seconds + shm->config.end_time);

    printf("Sim window: %02d:%02d → %02d:%02d (%d loaded flight plans)\n",
           sim_start_h, sim_start_m,
           sim_end_s / 3600, (sim_end_s % 3600) / 60,
           shm->num_flights);

    for (int i = 0; i < shm->num_flights; i++) {
        Flight *f = &shm->flights[i];
        bool in_scope = (f->departure_offset_s >= 0.0 && f->departure_offset_s < shm->config.end_time);
        if (in_scope) {
            num_forked++;
        } else {
            f->is_active = false;
            printf("  [SKIP] %s: departure offset %.0fs is outside sim window [0, %.0fs]\n",
                   f->callsign, f->departure_offset_s, shm->config.end_time);
        }
    }
    printf("%d / %d flight plans in scope — forking %d child processes.\n",
           num_forked, shm->num_flights, num_forked);

    int total_steps = (int)((shm->config.end_time - shm->config.start_time) / shm->config.time_step);

    pid_t parent_pid = getpid();
    sem_t *step_sems[MAX_FLIGHTS];
    sem_t *main_sem;
    init_semaphores(parent_pid, shm->num_flights, step_sems, &main_sem);
    shm->current_time = shm->config.start_time;

    int pipe_fds[MAX_FLIGHTS][2];
    for (int i = 0; i < shm->num_flights; i++) {
        if (pipe(pipe_fds[i]) < 0) {
            perror("pipe failed");
            return EXIT_FAILURE;
        }
    }

    pid_t pids[MAX_FLIGHTS];
    memset(pids, 0, sizeof(pids));

    for (int i = 0; i < shm->num_flights; i++) {
        shm->flights[i].process_id = 0;

        if (!shm->flights[i].is_active)
            continue;

        fflush(stdout);
        pid_t pid = fork();
        if (pid < 0) {
            perror("fork failed");
            cleanup(shm, pids, shm->num_flights, parent_pid, step_sems, main_sem);
            return EXIT_FAILURE;
        }

        if (pid == 0) {
            for (int j = 0; j < shm->num_flights; j++) {
                close(pipe_fds[j][0]);
                if (j != i) close(pipe_fds[j][1]);
            }
            run_flight_process(shm, i, pipe_fds[i][1], step_sems[i], main_sem);
            close(pipe_fds[i][1]);
            exit(EXIT_SUCCESS);
        }

        pids[i] = pid;
        shm->flights[i].process_id = pid;
        printf("Forked child PID %d for flight %s (departs at offset %.0fs)\n",
               pid, shm->flights[i].callsign, shm->flights[i].departure_offset_s);
    }

    // Parent only reads from pipes; keeping write ends open would prevent EOF.
    for (int i = 0; i < shm->num_flights; i++)
        close(pipe_fds[i][1]);

    // Wait for every child to signal ready before starting the step loop.
    for (int i = 0; i < num_forked; i++)
        sem_wait(main_sem);

    FlightPhase prev_phase[MAX_FLIGHTS];
    for (int i = 0; i < shm->num_flights; i++)
        prev_phase[i] = PHASE_GATE;

    PipeMessage step_msgs[MAX_FLIGHTS];

    static PositionHistory histories[MAX_FLIGHTS];
    for (int i = 0; i < MAX_FLIGHTS; i++) { histories[i].count = 0; histories[i].next = 0; }

    for (int i = 0; i < shm->num_flights; i++) {
        Flight *f = &shm->flights[i];
        Position dep = f->flight_plan.waypoints[0].pos;
        dep.timestamp = 0.0;
        append_position(&histories[i], &dep);
    }

    printf("\n--- Simulation running ---\n");

    {
        int wall_s = (int)shm->config.sim_start_seconds;
        printf("\n");
        printf("╔══════════════════════════════════════════════════════════════════════"
               "════════════════════════════════════════════════════════════════════╗\n");
        printf("║  t=       0 s  [%02d:%02d]  |  step   0 / %-3d  |  active= 0  pending=%2d  |  violations=  0"
               "                                                      ║\n",
               wall_s / 3600, (wall_s % 3600) / 60,
               (int)((shm->config.end_time - shm->config.start_time) / shm->config.time_step),
               num_forked);
        printf("╚══════════════════════════════════════════════════════════════════════"
               "════════════════════════════════════════════════════════════════════╝\n");
        printf("\n");
        printf("%-8s %-7s %8s %9s %7s %8s %5s %6s %8s %9s %6s  %6s %5s %7s  %6s %6s  %8s %8s  %8s %8s  %7s  %6s %7s  %6s %6s\n",
               "CALLSGN","PHASE","LAT(deg)","LON(deg)","ALT(m)","DEST(km)","CA(m)",
               "T(K)","P(Pa)","rho","a(m/s)",
               "IAS(kt)","Mach","TAS(m/s)",
               "CL","CD","Lift(N)","Drag(N)",
               "Te(N)","Tt(N)","VS(m/s)",
               "dF(kg)","dD(km)","M(kg)","Frem(kg)");
        printf("%-8s %-7s %8s %9s %7s %8s %5s %6s %8s %9s %6s  %6s %5s %7s  %6s %6s  %8s %8s  %8s %8s  %7s  %6s %7s  %6s %6s\n",
               "--------","-------","--------","---------","-------","--------","-----",
               "------","--------","---------","------",
               "------","-----","-------",
               "------","------","--------","--------",
               "--------","--------","-------",
               "------","-------","------","-------");
        for (int i = 0; i < shm->num_flights; i++) {
            Flight *f = &shm->flights[i];
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
                   f->current_pos.altitude,
                   dist_to_dest / 1000.0, cruise_alt,
                   "-","-","-","-", "-","-","-", "-","-","-","-", "-","-","-", "-","-",
                   f->initial_mass, fuel_rem);
        }
    }

    int step_num = 0;

    while (shm->current_time < shm->config.end_time) {

        double step_time = shm->current_time;
        int active = 0;
        bool was_active[MAX_FLIGHTS];
        for (int i = 0; i < shm->num_flights; i++) {
            was_active[i] = shm->flights[i].is_active && step_time >= shm->flights[i].departure_offset_s;
            if (was_active[i]) active++;
        }

        if (active == 0) {
            bool any_pending = false;
            for (int i = 0; i < shm->num_flights; i++) {
                if (shm->flights[i].is_active) {
                    any_pending = true;
                    break;
                }
            }
            if (!any_pending) break;
            shm->current_time += shm->config.time_step;
            continue;
        }

        step_num++;

        for (int i = 0; i < shm->num_flights; i++)
            if (was_active[i])
                sem_post(step_sems[i]);

        for (int i = 0; i < active; i++)
            sem_wait(main_sem);

        for (int i = 0; i < shm->num_flights; i++) {
            if (!was_active[i]) continue;
            ssize_t n = read(pipe_fds[i][0], &step_msgs[i], sizeof(PipeMessage));

            if (n == (ssize_t)sizeof(PipeMessage) &&
                step_msgs[i].type == MSG_POSITION_UPDATE) {
                if (is_valid_position(&step_msgs[i].new_pos)) {
                    append_position(&histories[i], &step_msgs[i].new_pos);
                } else {
                    printf("[WARN] Flight %d sent invalid position "
                           "(lat=%.4f lon=%.4f alt=%.2f) — update rejected\n",
                           step_msgs[i].flight_id,
                           step_msgs[i].new_pos.latitude,
                           step_msgs[i].new_pos.longitude,
                           step_msgs[i].new_pos.altitude);
                }
            }
        }

        shm->current_time += shm->config.time_step;

        // Children are blocked on step semaphores here, so shared memory is safe to read.
        check_violations(shm, was_active, pids, shm->num_flights);

        if (shm->termination_requested) {
            printf("[SIM] Simulation terminated early — max violations exceeded.\n");
            break;
        }

        print_step_table(shm, step_msgs, prev_phase,
                         shm->current_time, step_num, total_steps, step_time,
                         was_active);

        for (int i = 0; i < shm->num_flights; i++) {
            if (!was_active[i] || shm->flights[i].is_active) continue;
            Flight *f = &shm->flights[i];
            double remaining_fuel = f->current_mass - f->properties.empty_weight - f->payload;
            printf("[%s] Landed at destination. Remaining fuel: %.0f kg | Total mass: %.0f kg\n",
                   f->callsign, remaining_fuel, f->current_mass);
            print_arrival_deviation(f->legs[f->current_leg].scheduled_arrival_offset_s,
                                    shm->current_time,
                                    shm->config.sim_start_seconds);
        }

        // Child is blocked on step_sem here — no concurrent write on leg_just_completed.
        bool leg_done[MAX_FLIGHTS];
        for (int i = 0; i < shm->num_flights; i++) leg_done[i] = false;
        for (int i = 0; i < shm->num_flights; i++) {
            if (!was_active[i] || !shm->flights[i].leg_just_completed) continue;
            shm->flights[i].leg_just_completed = false;
            leg_done[i] = true;
            Flight *f = &shm->flights[i];
            printf("[%s] Arrived at intermediate stop. Leg %d complete. "
                   "Next leg (leg %d) departs at offset %.0fs.\n",
                   f->callsign,
                   f->current_leg - 1,
                   f->current_leg,
                   f->departure_offset_s);
            print_arrival_deviation(f->legs[f->current_leg - 1].scheduled_arrival_offset_s,
                                    shm->current_time,
                                    shm->config.sim_start_seconds);
        }

        fflush(stdout);

        for (int i = 0; i < shm->num_flights; i++)
            if (was_active[i])
                prev_phase[i] = step_msgs[i].step.phase;

        // Flights that just completed a leg are back at the gate.
        for (int i = 0; i < shm->num_flights; i++)
            if (leg_done[i])
                prev_phase[i] = PHASE_GATE;
    }

    finalize_simulation_report(shm, shm->current_time);

    printf("\n");
    printf("╔══════════════════════════════════════════════════════╗\n");
    printf("║              SIMULATION COMPLETE                     ║\n");
    printf("╚══════════════════════════════════════════════════════╝\n");
    printf("Total violations detected: %d\n", shm->total_violations_detected);
    for (int i = 0; i < shm->num_flights; i++) {
        bool in_scope = (shm->flights[i].legs[0].departure_offset_s >= 0.0 &&
                         shm->flights[i].legs[0].departure_offset_s < shm->config.end_time);
        if (!in_scope) continue;
        printf("  %-8s : %s\n",
               shm->flights[i].callsign,
               shm->flights[i].violation_detected ? "HAD VIOLATION" : "clean");
    }

    printf("\n");
    printf("╔══════════════════════════════════════════════════════╗\n");
    printf("║           POSITION HISTORY (US101)                   ║\n");
    printf("╚══════════════════════════════════════════════════════╝\n");
    for (int i = 0; i < shm->num_flights; i++) {
        bool in_scope = (shm->flights[i].legs[0].departure_offset_s >= 0.0 &&
                         shm->flights[i].legs[0].departure_offset_s < shm->config.end_time);
        if (!in_scope || histories[i].count == 0) continue;
        printf("\n  %s  (%d steps)\n", shm->flights[i].callsign, histories[i].count);
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

    for (int i = 0; i < shm->num_flights; i++)
        close(pipe_fds[i][0]);

    cleanup(shm, pids, shm->num_flights, parent_pid, step_sems, main_sem);
    return EXIT_SUCCESS;
}
