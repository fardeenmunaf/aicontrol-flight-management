#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <fcntl.h>          // O_CREAT, O_RDWR — flags for shm_open and sem_open
#include <sys/mman.h>       // mmap, munmap, PROT_*, MAP_SHARED
#include <sys/stat.h>       // mode constants (0666)
#include <sys/wait.h>       // waitpid
#include <unistd.h>         // fork, pipe, read, write, close, getpid
#include <semaphore.h>      // sem_open, sem_wait, sem_post, sem_close, sem_unlink
#include "structs.h"
#include "json_parser.h"
#include "flight_simulation.h"
#include "position_tracker.h"

// Name of the POSIX shared memory object that all processes share.
// The leading '/' is required by the POSIX spec for shm_open names.
#define SHM_NAME "/aisafe_sim"

// ── SECTION 1 — Shared memory setup ───────────────────────────────────────────
//
// POSIX shared memory flow:
//   shm_open  → creates/opens a named shared memory object (like opening a file)
//   ftruncate → sets its size (the object starts empty, with size 0)
//   mmap      → maps the object into this process's virtual address space
//   close     → the file descriptor is no longer needed after mmap
//
// The key is MAP_SHARED: writes by any process are immediately visible to all
// other processes that have mmap'd the same object — zero copying, zero overhead.
// This mapping is inherited by children created with fork().

static SharedMemoryData *setup_shared_memory(void) {
    // Remove any leftover object from a previous crashed run.
    // shm_unlink() on a non-existent name returns -1 (silently ignored here).

    shm_unlink(SHM_NAME);

    // Create (O_CREAT) a new shared memory object, readable and writable (O_RDWR).
    // 0666 = permissions: owner/group/others can read+write.

    int fd = shm_open(SHM_NAME, O_CREAT | O_RDWR, 0666);
    if (fd < 0) {
        perror("shm_open failed");
        exit(EXIT_FAILURE);
    }

    // The object starts with size 0 — resize it to hold our SharedMemoryData struct.
    if (ftruncate(fd, sizeof(SharedMemoryData)) < 0) {
        perror("ftruncate failed");
        exit(EXIT_FAILURE);
    }

    // Map the shared memory object into our virtual address space.
    // NULL  = kernel chooses the address
    // MAP_SHARED = writes are visible to other processes (vs MAP_PRIVATE = copy-on-write)
    // fd, 0 = map from offset 0 of the shared memory object

    SharedMemoryData *shm = mmap(NULL, sizeof(SharedMemoryData),PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0); //address, bytes, permissions, share, object, offset

    // The fd is only needed to create the mapping; once mmap() succeeds, close it.
    // The mapping remains alive until munmap() is called — closing fd doesn't unmap it.

    close(fd);

    if (shm == MAP_FAILED) {
        perror("mmap failed");
        exit(EXIT_FAILURE);
    }

    memset(shm, 0, sizeof(SharedMemoryData)); // zero-initialise (clear any garbage)
    return shm;
}

// ── SECTION 2 — Named semaphore initialisation ────────────────────────────────
//
// Named POSIX semaphores (sem_open) — taught in SCOMP module 6.
//
// We use TWO kinds of semaphores, implementing the EXECUTION ORDER pattern
// from the lectures (initialised to 0 so the first sem_wait always blocks):
//
//   step_sems[i]  — one per flight. Parent posts → child i unblocks and runs a step.
//   main_sem      — shared by all. Each child posts when done → parent counts them.
//
// Names include the parent PID so that two simultaneous simulation runs
// do not accidentally share semaphores (would cause deadlock).
//
// After fork(), child processes inherit the parent's sem_t* pointers.
// POSIX guarantees that named semaphore descriptors survive fork(),
// so children can call sem_wait/sem_post on the inherited pointers directly.

static void init_semaphores(pid_t pid, int n,
                             sem_t **step_sems, sem_t **main_sem_out) {
    for (int i = 0; i < n; i++) {
        char name[48];
        // Build a unique name per flight, e.g. "/aisafe_step_12345_0"
        snprintf(name, sizeof(name), "/aisafe_step_%d_%d", (int)pid, i);

        // Remove any stale semaphore with this name (previous crash)
        sem_unlink(name);

        // O_CREAT = create if not existing; 0666 = permissions; 0 = initial value.
        // Initial value 0 means: child calls sem_wait() → blocks immediately.
        // It will only unblock when the parent calls sem_post().

        step_sems[i] = sem_open(name, O_CREAT, 0666, 0);
        if (step_sems[i] == SEM_FAILED) {
            perror("sem_open step");
            exit(EXIT_FAILURE);
        }
    }

    // One shared semaphore for all children to post to when their step is done.
    char main_name[48];
    snprintf(main_name, sizeof(main_name), "/aisafe_main_%d", (int)pid);
    sem_unlink(main_name);
    *main_sem_out = sem_open(main_name, O_CREAT, 0666, 0); // also starts at 0
    if (*main_sem_out == SEM_FAILED) {
        perror("sem_open main");
        exit(EXIT_FAILURE);
    }
}

// ── SECTION 3 — Violation detection ───────────────────────────────────────────
//
// After every time step the parent checks every pair of active flights.
// A violation is flagged when the 3-D Euclidean distance between two aircraft
// drops below the configured safety_distance_threshold.
//
// We use a flat-earth approximation to convert lat/lon differences to metres:
//   dx (east-west) = Δlon * cos(avg_lat) * R     — cos corrects for longitude
//   dy (north-south) = Δlat * R                   — convergence at the poles
// This is accurate within a few percent for separations under ~100 km.

static void check_violations(SharedMemoryData *shm, bool *was_active) {
    const double R = 6371000.0; // Earth's mean radius in metres

    // ── proximity check ───────────────────────────────────────────────────────────
    // Compare every unique pair (i, j) among flights that ran this step.
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
            }
        }
    }

    // ── fuel exhaustion check ─────────────────────────────────────────────────────
    // Report once per flight the first time it lands (or is caught) with 0 kg remaining.
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
}

// ── SECTION 4 — Per-step output ───────────────────────────────────────────────
//
// Prints the full 25-column physics table for every active flight after each step.
// The parent collects all PipeMessage structs first (via drain_pipes), then calls
// this function once to produce clean, non-interleaved output.
//
// Columns (25):
//   callsign, phase, lat, lon, alt, d_dest, cruise_alt,
//   T, P, rho, a,
//   IAS, Mach, TAS,
//   CL, CD, Lift, Drag,
//   Te, Tt, VS,
//   dFuel, dDist, Mass, Fuel_rem

static const char *phase_name(FlightPhase p) {
    switch (p) {
        case PHASE_GATE:    return "GATE   ";
        case PHASE_CLIMB:   return "CLIMB  ";
        case PHASE_CRUISE:  return "CRUISE ";
        case PHASE_DESCEND: return "DESCEND";
        default:            return "???????";
    }
}

static void print_step_table(SharedMemoryData *shm,
                              PipeMessage *msgs,     // one per flight (indexed by flight slot)
                              FlightPhase *prev_ph,  // previous phase per flight
                              double sim_time,
                              int step_num, int total_steps,
                              double step_time,
                              bool *was_active) {    // which flights were active at step start

    // Count flights still airborne AFTER this step (for the banner)
    int active = 0;
    for (int i = 0; i < shm->num_flights; i++)
        if (shm->flights[i].is_active && step_time >= shm->flights[i].departure_offset_s)
            active++;
    // Also count flights still waiting to depart (not yet woken) — informational
    int pending = 0;
    for (int i = 0; i < shm->num_flights; i++)
        if (shm->flights[i].is_active && step_time < shm->flights[i].departure_offset_s)
            pending++;

    // ── step banner ───────────────────────────────────────────────────────────
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

    // ── phase transition banners ──────────────────────────────────────────────
    // Use was_active so that a flight landing this step (is_active just became
    // false) still shows its final phase transition (e.g. CRUISE → DESCEND).
    for (int i = 0; i < shm->num_flights; i++) {
        if (!was_active[i]) continue;
        FlightPhase cur = msgs[i].step.phase;
        if (cur != prev_ph[i]) {
            const char *from = phase_name(prev_ph[i]);
            const char *to   = phase_name(cur);
            printf("  >>> %s: %s --> %s <<<\n",
                   shm->flights[i].callsign, from, to);
        }
    }

    // ── column header ─────────────────────────────────────────────────────────
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

    // ── one row per flight that was active this step ───────────────────────────
    // Including flights that just landed — their pipe message carries the landing
    // step data (DESCEND phase, final position) so it's visible in the table.
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

// ── SECTION 5 — Simulation cleanup ────────────────────────────────────────────
//
// Everything created must be explicitly destroyed (POSIX resources don't
// disappear when a process exits — they persist until unlinked or the system reboots).
//
//   sem_close  → decrements the reference count for THIS process
//   sem_unlink → removes the name from the system (like deleting a file)
//   munmap     → unmaps the shared memory from this process's address space
//   shm_unlink → removes the shared memory object name from /dev/shm

static void cleanup(SharedMemoryData *shm, pid_t *pids, int n,
                    pid_t parent_pid, sem_t **step_sems, sem_t *main_sem) {

    // Tell all children to exit on their next sem_wait() check
    shm->termination_requested = true;

    // Unblock any child that is currently blocked on sem_wait(step_sem).
    // Without this, children would sleep forever and waitpid() would hang.

    for (int i = 0; i < n; i++)
        sem_post(step_sems[i]);

    // Wait for every child process to finish (reap zombies).
    // NULL = we don't care about the exit status; 0 = block until done.

    for (int i = 0; i < n; i++)
        if (pids[i] > 0) waitpid(pids[i], NULL, 0);

    // Close and remove each step semaphore
    for (int i = 0; i < n; i++) {
        sem_close(step_sems[i]);    // close this process's reference
        char name[48];
        snprintf(name, sizeof(name), "/aisafe_step_%d_%d", (int)parent_pid, i);
        sem_unlink(name);           // delete the semaphore from the system
    }

    // Close and remove the main semaphore
    char main_name[48];
    snprintf(main_name, sizeof(main_name), "/aisafe_main_%d", (int)parent_pid);
    sem_close(main_sem);
    sem_unlink(main_name);

    // Unmap the shared memory from this process's address space
    munmap(shm, sizeof(SharedMemoryData));

    // Remove the shared memory object from /dev/shm
    shm_unlink(SHM_NAME);
}

// ── SECTION 6 — Main entry point ──────────────────────────────────────────────

int main() {

    printf("=== AISafe Simulation Engine starting ===\n");

    // ── 6.1  Create shared memory BEFORE fork ─────────────────────────────────
    // Must happen before fork() so children inherit the mmap'd region.
    // After fork(), parent and children all point to the same physical memory.

    SharedMemoryData *shm = setup_shared_memory();

    // ── 6.2  Load configuration and flight plans ───────────────────────────────
    // Data is written directly into shared memory, so all forked children
    // will already have the flight plans without any extra IPC.

    load_simulation_settings("simulation_settings.json", &shm->config);
    load_all_flight_plans("flightplansSim", shm);
    printf("Loaded %d flight(s). Simulation: %.0f s -> %.0f s  (step %.0f s = %.0f min)\n",
           shm->num_flights,
           shm->config.start_time, shm->config.end_time,
           shm->config.time_step, shm->config.time_step / 60.0);

    // ── Scope validation: mark out-of-window flights inactive, count forkable ones ──
    int num_forked = 0;
    int sim_start_h = (int)(shm->config.sim_start_seconds / 3600);
    int sim_start_m = (int)(fmod(shm->config.sim_start_seconds, 3600.0) / 60.0);
    int sim_end_s   = (int)(shm->config.sim_start_seconds + shm->config.end_time);

    printf("Sim window: %02d:%02d → %02d:%02d (%d loaded flight plans)\n", sim_start_h, sim_start_m, sim_end_s / 3600, (sim_end_s % 3600) / 60, shm->num_flights);

    for (int i = 0; i < shm->num_flights; i++) 
    {
        Flight *f = &shm->flights[i];
        bool in_scope = (f->departure_offset_s >= 0.0 && f->departure_offset_s < shm->config.end_time);
        if (in_scope) {
            num_forked++;
        } else {
            f->is_active = false;
            printf("  [SKIP] %s: departure offset %.0fs is outside sim window [0, %.0fs]\n", f->callsign, f->departure_offset_s, shm->config.end_time);
        }
    }
    printf("%d / %d flight plans in scope — forking %d child processes.\n", num_forked, shm->num_flights, num_forked);

    int total_steps = (int)((shm->config.end_time - shm->config.start_time) / shm->config.time_step);

    // ── 6.3  Create named semaphores BEFORE fork ───────────────────────────────
    // sem_t* pointers are inherited by children after fork().
    // Each child only needs its own step_sems[i] and the shared main_sem.
    pid_t parent_pid = getpid(); // used to construct unique semaphore names

    sem_t *step_sems[MAX_FLIGHTS];
    sem_t *main_sem;
    init_semaphores(parent_pid, shm->num_flights, step_sems, &main_sem); //name, number, array of step semaphores, main semaphore
    shm->current_time = shm->config.start_time;

    // ── 6.4  Create pipes BEFORE fork ─────────────────────────────────────────
    //
    // One pipe per flight:
    //   pipe_fds[i][0] = read end  — parent reads from here
    //   pipe_fds[i][1] = write end — child i writes to here
    //
    // Pipes are one-way byte streams (SCOMP module 4).
    // Creating them before fork() ensures both ends exist in both parent and child.
    // After fork, each side closes the end it doesn't use.

    int pipe_fds[MAX_FLIGHTS][2];
    for (int i = 0; i < shm->num_flights; i++) {
        if (pipe(pipe_fds[i]) < 0) {
            perror("pipe failed");
            return EXIT_FAILURE;
        }
    }

    // ── fork one child per flight ──────────────────────────────────────────────
    pid_t pids[MAX_FLIGHTS];
    memset(pids, 0, sizeof(pids)); // 0 = not forked; set to child PID when forked

    for (int i = 0; i < shm->num_flights; i++) {
        shm->flights[i].process_id = 0;

        if (!shm->flights[i].is_active) {
            // Out-of-scope flight — no child process needed
            continue;
        }

        fflush(stdout);
        pid_t pid = fork();
        if (pid < 0) {
            perror("fork failed");
            cleanup(shm, pids, shm->num_flights, parent_pid, step_sems, main_sem);
            return EXIT_FAILURE;
        }

        if (pid == 0) {
            // ════════════ CHILD PROCESS ════════════════════════════════════════
            for (int j = 0; j < shm->num_flights; j++) {
                close(pipe_fds[j][0]); //close read ends
                if (j != i) close(pipe_fds[j][1]); //close write ends of other flights
            }
            run_flight_process(shm, i, pipe_fds[i][1], step_sems[i], main_sem);
            close(pipe_fds[i][1]);
            exit(EXIT_SUCCESS);
            // ════════════ END OF CHILD ═════════════════════════════════════════
        }

        pids[i] = pid;
        shm->flights[i].process_id = pid; //set process id for flight
        printf("Forked child PID %d for flight %s (departs at offset %.0fs)\n", pid, shm->flights[i].callsign, shm->flights[i].departure_offset_s);
    }

    // Parent closes ALL write ends — it only reads from the pipes.
    // Important: if the parent kept a write end open, read() would never return EOF.

    for (int i = 0; i < shm->num_flights; i++)
    {
        close(pipe_fds[i][1]);
    }

    // Startup barrier: wait for every child to post main_sem once (signals ready).
    // Guarantees all "Process started" lines appear before any simulation step output.
    for (int i = 0; i < num_forked; i++)
    {
        sem_wait(main_sem);
    }

    // Initialize all flights as GATE so the first active step always prints
    // GATE → CLIMB, regardless of what current_phase the child set initially.
    FlightPhase prev_phase[MAX_FLIGHTS];
    for (int i = 0; i < shm->num_flights; i++)
    {
        prev_phase[i] = PHASE_GATE;
    }

    // Storage for one PipeMessage per flight per step (re-used each iteration).
    PipeMessage step_msgs[MAX_FLIGHTS];

    // Position history: one buffer per flight, filled by the parent as
    // it reads validated updates from each flight's pipe
    static PositionHistory histories[MAX_FLIGHTS];
    for (int i = 0; i < MAX_FLIGHTS; i++) histories[i].count = 0;

    // ── 6.5  Step-lock simulation loop (parent only) ───────────────────────────
    //
    // This loop advances the simulation one time step at a time.
    // Each iteration:
    //   1. Post to each active child's step semaphore → children run in parallel
    //   2. Wait for each child to post back to main_sem → all children are done
    //   3. Read each child's pipe message → collect StepData for the UI table
    //   4. Check for violations → compare all aircraft pairs
    //   5. Advance the clock
    //   6. Print the full 25-column table for this step
    //
    // This is the STEP-LOCK pattern: the simulation clock only moves forward
    // after every active process has finished computing the current step.

    printf("\n--- Simulation running ---\n");

    // ── Step 0: print initial state before any physics runs ───────────────────
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

        // Snapshot which flights this step should wake: active AND past departure.
        // Pre-departure children stay blocked on their semaphore and cost nothing.
        // was_active also captures flights that land THIS step (is_active flips to
        // false inside the child, but we must still drain their pipe and show them).
        double step_time = shm->current_time;
        int active = 0;
        bool was_active[MAX_FLIGHTS];
        for (int i = 0; i < shm->num_flights; i++) {
            was_active[i] = shm->flights[i].is_active && step_time >= shm->flights[i].departure_offset_s;
            if (was_active[i]) active++;
        }

        if (active == 0) {
            // No flying flights this step — check if any are still waiting to depart
            bool any_pending = false;
            for (int i = 0; i < shm->num_flights; i++)
            {
                if (shm->flights[i].is_active) 
                { 
                    any_pending = true; 
                    break; 
                }
            }
            if (!any_pending) break;
            shm->current_time += shm->config.time_step;
            continue;
        }

        step_num++;

        // ── SIGNAL: wake only the flights flying this step ────────────────────
        for (int i = 0; i < shm->num_flights; i++)
        {
            if (was_active[i])
            {
                sem_post(step_sems[i]);
            }
        }

        // ── WAIT: block until every woken child finishes ──────────────────────
        for (int i = 0; i < active; i++)
        {
            sem_wait(main_sem);
        }

        // ── DRAIN pipes ───────────────────────────────────────────────────────
        // Read from all flights that ran this step, including those that just
        // landed (is_active is now false but the pipe message was still written).
        for (int i = 0; i < shm->num_flights; i++) 
        {
            if (!was_active[i]) continue;
            ssize_t n = read(pipe_fds[i][0], &step_msgs[i], sizeof(PipeMessage)); //read (pipe of the child, store in array, size)

            // Validate the received position and track it.
            // A complete, valid update is appended to this flight's history so that
            // past positions are available for safety analysis .
            // An incomplete read or invalid coordinates are rejected without
            // corrupting the stored data (TC05).
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

        // ── ADVANCE the simulation clock ──────────────────────────────────────
        shm->current_time += shm->config.time_step;

        // ── CHECK for safety and fuel violations ──────────────────────────────
        check_violations(shm, was_active);

        // ── PRINT the full per-step table ─────────────────────────────────────
        print_step_table(shm, step_msgs, prev_phase,
                         shm->current_time, step_num, total_steps, step_time,
                         was_active);

        for (int i = 0; i < shm->num_flights; i++) {
            if (!was_active[i] || shm->flights[i].is_active) continue;
            Flight *f = &shm->flights[i];
            double remaining_fuel = f->current_mass - f->properties.empty_weight - f->payload;
            printf("[%s] Landed at destination. Remaining fuel: %.0f kg | Total mass: %.0f kg\n",
                   f->callsign, remaining_fuel, f->current_mass);
        }

        // Intermediate leg completion: child set leg_just_completed=true and is_active=true.
        // Parent clears the flag here (child is blocked on step_sem — no concurrent write).
        // Track which flights completed a leg so prev_phase can be reset to GATE below.
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
                   f->current_leg - 1,   // just-completed leg (already incremented in child)
                   f->current_leg,
                   f->departure_offset_s);
        }

        fflush(stdout);

        // ── UPDATE prev_phase for next iteration ──────────────────────────────
        for (int i = 0; i < shm->num_flights; i++)
            if (was_active[i])
                prev_phase[i] = step_msgs[i].step.phase;
        // Flights that just completed a leg are back at the gate — next step
        // should show GATE → CLIMB rather than (old phase) → CLIMB.
        for (int i = 0; i < shm->num_flights; i++)
            if (leg_done[i])
                prev_phase[i] = PHASE_GATE;
    }

    // ── 6.6  Wrap up and report ────────────────────────────────────────────────
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

    // ── US101 — Position history report ───────────────────────────────────────
    printf("\n");
    printf("╔══════════════════════════════════════════════════════╗\n");
    printf("║           POSITION HISTORY (US101)                   ║\n");
    printf("╚══════════════════════════════════════════════════════╝\n");
    printf("%-8s  %6s  %s\n", "FLIGHT", "STEPS", "LAST KNOWN POSITION");
    printf("%-8s  %6s  %s\n", "--------", "------", "--------------------------------------------");
    for (int i = 0; i < shm->num_flights; i++) {
        if (histories[i].count == 0) continue;
        Position *last = &histories[i].entries[histories[i].count - 1];
        printf("%-8s  %6d  lat=%8.4f  lon=%9.4f  alt=%7.0f m\n",
               shm->flights[i].callsign,
               histories[i].count,
               last->latitude,
               last->longitude,
               last->altitude);
    }
    printf("\n");

    // Close the parent's pipe read ends before freeing shared memory
    for (int i = 0; i < shm->num_flights; i++)
        close(pipe_fds[i][0]);

    // Release all resources: wait for children, destroy semaphores, unmap/unlink memory
    cleanup(shm, pids, shm->num_flights, parent_pid, step_sems, main_sem);
    return EXIT_SUCCESS;
}
