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

// Name of the POSIX shared memory object that all processes share.
// The leading '/' is required by the POSIX spec for shm_open names.
// This name appears in /dev/shm/ on Linux.
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

    SharedMemoryData *shm = mmap(NULL, sizeof(SharedMemoryData),
                                 PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);

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

static void check_violations(SharedMemoryData *shm) {
    const double R = 6371000.0; // Earth's mean radius in metres

    // Compare every unique pair (i, j) — no need to check (j, i) again
    for (int i = 0; i < shm->num_flights; i++) {
        if (!shm->flights[i].is_active) continue; // skip landed flights

        for (int j = i + 1; j < shm->num_flights; j++) {
            if (!shm->flights[j].is_active) continue;

            Position *a = &shm->flights[i].current_pos;
            Position *b = &shm->flights[j].current_pos;

            // Average latitude in radians — needed for the longitude correction
            double lat_avg = (a->latitude + b->latitude) / 2.0 * M_PI / 180.0;

            // East-west distance: degree difference × metres-per-degree at this latitude
            double dx = (b->longitude - a->longitude) * M_PI / 180.0 * cos(lat_avg) * R;

            // North-south distance: degree difference × metres-per-degree
            double dy = (b->latitude  - a->latitude)  * M_PI / 180.0 * R;

            // Vertical separation — already in metres
            double dz = b->altitude - a->altitude;

            // 3-D Euclidean distance
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
                              int step_num, int total_steps) {

    int active = 0;
    for (int i = 0; i < shm->num_flights; i++)
        if (shm->flights[i].is_active) active++;

    // ── step banner ───────────────────────────────────────────────────────────
    printf("\n");
    printf("╔══════════════════════════════════════════════════════════════════════"
           "════════════════════════════════════════════════════════════════════╗\n");
    printf("║  t=%7.0f s  |  step %3d / %-3d  |  active=%2d  |  violations=%3d         "
           "                                                               ║\n",
           sim_time, step_num, total_steps,
           active, shm->total_violations_detected);
    printf("╚══════════════════════════════════════════════════════════════════════"
           "════════════════════════════════════════════════════════════════════╝\n");

    // ── phase transition banners ──────────────────────────────────────────────
    for (int i = 0; i < shm->num_flights; i++) {
        if (!shm->flights[i].is_active) continue;
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

    // ── one row per active flight ─────────────────────────────────────────────
    for (int i = 0; i < shm->num_flights; i++) {
        if (!shm->flights[i].is_active) continue;

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
        waitpid(pids[i], NULL, 0);

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

int main(int argc, char **argv) {
    (void)argc; (void)argv; // suppress unused-parameter warnings

    printf("=== AISafe Simulation Engine starting ===\n");

    // ── 6.1  Create shared memory BEFORE fork ─────────────────────────────────
    // Must happen before fork() so children inherit the mmap'd region.
    // After fork(), parent and children all point to the same physical RAM.

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
    int wall_start_h = (int)(shm->config.sim_start_seconds / 3600);
    int wall_start_m = (int)(fmod(shm->config.sim_start_seconds, 3600.0) / 60.0);
    int wall_end_s   = (int)(shm->config.sim_start_seconds + shm->config.end_time);
    printf("Sim window: %02d:%02d → %02d:%02d (%d loaded flight plans)\n",
           wall_start_h, wall_start_m,
           wall_end_s / 3600, (wall_end_s % 3600) / 60,
           shm->num_flights);

    for (int i = 0; i < shm->num_flights; i++) {
        Flight *f = &shm->flights[i];
        bool in_scope = (f->departure_offset_s >= 0.0 &&
                         f->departure_offset_s < shm->config.end_time);
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

    int total_steps = (int)((shm->config.end_time - shm->config.start_time)
                             / shm->config.time_step);

    // ── 6.3  Create named semaphores BEFORE fork ───────────────────────────────
    // sem_t* pointers are inherited by children after fork().
    // Each child only needs its own step_sems[i] and the shared main_sem.
    pid_t parent_pid = getpid(); // used to construct unique semaphore names

    sem_t *step_sems[MAX_FLIGHTS];
    sem_t *main_sem;
    init_semaphores(parent_pid, shm->num_flights, step_sems, &main_sem);
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
                close(pipe_fds[j][0]);
                if (j != i) close(pipe_fds[j][1]);
            }
            run_flight_process(shm, i, pipe_fds[i][1], step_sems[i], main_sem);
            close(pipe_fds[i][1]);
            exit(EXIT_SUCCESS);
            // ════════════ END OF CHILD ═════════════════════════════════════════
        }

        pids[i] = pid;
        shm->flights[i].process_id = pid;
        printf("Forked child PID %d for flight %s (departs at offset %.0fs)\n",
               pid, shm->flights[i].callsign, shm->flights[i].departure_offset_s);
    }

    // Parent closes ALL write ends — it only reads from the pipes.
    // Important: if the parent kept a write end open, read() would never return EOF.

    for (int i = 0; i < shm->num_flights; i++)
        close(pipe_fds[i][1]);

    // Startup barrier: wait for every child to post main_sem once (signals ready).
    // Guarantees all "Process started" lines appear before any simulation step output.
    for (int i = 0; i < num_forked; i++)
        sem_wait(main_sem);

    // After the startup barrier all children have set f->current_phase from their
    // initial position — read those values into prev_phase[] so transitions from
    // step 1 onward are correctly detected.
    FlightPhase prev_phase[MAX_FLIGHTS];
    for (int i = 0; i < shm->num_flights; i++)
        prev_phase[i] = shm->flights[i].current_phase;

    // Storage for one PipeMessage per flight per step (re-used each iteration).
    PipeMessage step_msgs[MAX_FLIGHTS];

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

    int step_num = 0;

    while (shm->current_time < shm->config.end_time) {

        // Count how many flights are still in the air this step
        int active = 0;
        for (int i = 0; i < shm->num_flights; i++)
            if (shm->flights[i].is_active) active++;

        if (active == 0) break; // all flights have landed — no more work to do

        step_num++;

        // ── SIGNAL: wake every active child ───────────────────────────────────
        // sem_post() increments each step semaphore by 1.
        // The child is blocked on sem_wait(step_sem) — this unblocks it.

        for (int i = 0; i < shm->num_flights; i++)
            if (shm->flights[i].is_active)
                sem_post(step_sems[i]);

        // ── WAIT: block until every active child finishes ─────────────────────
        // Each child posts main_sem once when it finishes its step.
        // We call sem_wait() once per active flight, so we unblock only after
        // ALL of them have posted — guaranteed synchronisation barrier.

        for (int i = 0; i < active; i++)
            sem_wait(main_sem);

        // ── DRAIN pipes: read one PipeMessage per active flight ───────────────
        // Each child wrote a PipeMessage after its step — it contains the new
        // position AND all 25 intermediate physics values (StepData).
        // We store each message so print_step_table() can format them cleanly.

        for (int i = 0; i < shm->num_flights; i++) {
            if (!shm->flights[i].is_active) continue;
            ssize_t n = read(pipe_fds[i][0], &step_msgs[i], sizeof(PipeMessage));
            (void)n; // read() blocks until full message arrives — size always matches
        }

        // ── ADVANCE the simulation clock ──────────────────────────────────────
        shm->current_time += shm->config.time_step;

        // ── CHECK for safety violations ───────────────────────────────────────
        check_violations(shm);

        // ── PRINT the full per-step table ─────────────────────────────────────
        print_step_table(shm, step_msgs, prev_phase,
                         shm->current_time, step_num, total_steps);

        // ── UPDATE prev_phase for next iteration ──────────────────────────────
        for (int i = 0; i < shm->num_flights; i++)
            if (shm->flights[i].is_active)
                prev_phase[i] = step_msgs[i].step.phase;
    }

    // ── 6.6  Wrap up and report ────────────────────────────────────────────────
    printf("\n");
    printf("╔══════════════════════════════════════════════════════╗\n");
    printf("║              SIMULATION COMPLETE                     ║\n");
    printf("╚══════════════════════════════════════════════════════╝\n");
    printf("Total violations detected: %d\n", shm->total_violations_detected);
    for (int i = 0; i < shm->num_flights; i++) {
        printf("  %-8s : %s\n",
               shm->flights[i].callsign,
               shm->flights[i].violation_detected ? "HAD VIOLATION" : "clean");
    }

    // Close the parent's pipe read ends before freeing shared memory
    for (int i = 0; i < shm->num_flights; i++)
        close(pipe_fds[i][0]);

    // Release all resources: wait for children, destroy semaphores, unmap/unlink memory
    cleanup(shm, pids, shm->num_flights, parent_pid, step_sems, main_sem);
    return EXIT_SUCCESS;
}
