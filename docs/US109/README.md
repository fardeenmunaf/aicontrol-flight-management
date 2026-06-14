# US109 — Generate and Store the Final Simulation Report

## User Story

> As a **Flight Control Operator**, I want the report thread to assemble a final summary of the simulation — including every flight's execution status and all recorded safety violations — and have it written to a file at the end of the run, so that I can review the outcome after the simulation finishes.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC1 | When the simulation's main loop finishes, the report thread is signalled to perform a final aggregation pass |
| AC2 | The final report includes, for every flight, whether it completed successfully, whether it had a violation, its remaining fuel, and legs completed |
| AC3 | The final report includes the overall validation result (pass/fail) based on the total number of recorded violations |
| AC4 | The report is written to a file (delegated to a separate process), and the parent process waits for that process to finish before exiting |
| AC5 | All synchronization primitives (mutex, condition variables, semaphores) used throughout the simulation are properly destroyed/released before the program exits |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe-simulation` — `structs.h` | `SimulationReport`, `FlightStatus`, `SimContext.report_finalize` |
| `aisafe-simulation` — `simulation_engine.c` | `main()` shutdown sequence, `finalize_and_write_report()`, `report_thread`'s finalize branch, `cleanup()` |
| `aisafe-simulation` — `report_generator.c` | Standalone binary, `execl`'d by `finalize_and_write_report`, serializes `SimulationReport` to JSON |
| `aisafe.app.javafx` | `SimulationReportParser`, `SimulationUI.showReportDialog()` — consumes the JSON (documented in detail in **US111**) |

---

## Architecture — Shutdown Sequence

```
main() — step loop ends (sim->current_time >= config.end_time, or termination_requested)
   │
   ├─ ctx.sim_done = true
   ├─ broadcast cond_environment / cond_dispatch / cond_results / cond_violations
   ├─ pthread_join(thr_environment, thr_dispatcher, thr_results, thr_violations)  ── AC5 (worker threads)
   │
   ├─ ctx.report_finalize = true
   ├─ pthread_cond_signal(&ctx.cond_report)               ── AC1
   │       │
   │       ▼
   │   report_thread wakes (report_finalize == true)
   │       │
   │       ├─ finalize_and_write_report(shm, ctx)         ── AC2/AC3/AC4
   │       │     ├─ populate sim->report.flight_statuses[]
   │       │     ├─ sim->report.total_flights = ...
   │       │     ├─ fork() + execl("./report_generator", "/aisafe_sim")
   │       │     └─ waitpid(report_generator_pid)
   │       └─ break  (thread exits)
   │
   ├─ pthread_join(thr_report)
   ├─ pthread_mutex_destroy + pthread_cond_destroy × 6     ── AC5
   ├─ cleanup(shm, pids, num_flights)                       ── (US108)
   ├─ print final summary (violations per flight, position history)
   └─ sem_close/sem_unlink × (2×MAX_FLIGHTS+1), munmap, shm_unlink  ── AC5
```

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

- `SimulationReport` is the **Aggregate Root** of the "simulation outcome" sub-domain: it owns `flight_statuses[]`, `violations[]`, `total_violations_recorded`, and `report_ready` as one consistent whole, finalized in a single pass (`finalize_and_write_report`) rather than incrementally — avoiding a partially-written report being read.
- Delegating the actual file write to a **separate process** (`report_generator`) mirrors a **bounded context boundary**: the simulation engine's responsibility ends at "the in-memory report is complete and consistent"; serialization-to-JSON is a different concern, owned by a different executable.

### Gang of Four (GoF) Patterns

| Pattern | Application | Benefit |
|---------|-------------|---------|
| **Builder** (informal) | `finalize_and_write_report` incrementally fills `sim->report` field by field (per-flight statuses, then totals) before marking it ready | A partially-built report is never observed by `report_generator` — it only runs after the builder completes |
| **Facade** | `finalize_and_write_report(shm, ctx)` hides the entire fork/exec/wait sequence behind one call from `report_thread` | `report_thread`'s finalize branch stays a two-line call + `break` |
| **Adapter** | `report_generator` adapts the in-memory C `SimulationReport`/`SimulationConfig` structs (read from shared memory) into a JSON document consumable by the JavaFX layer | The Java side never needs to understand C struct layouts |

### SOLID Principles

1. **Single Responsibility Principle (SRP)** — `finalize_and_write_report` only aggregates and delegates; the actual JSON serialization is entirely in `report_generator.c` (a different translation unit / binary).
2. **Open/Closed Principle (OCP)** — new fields can be added to `SimulationReport`/`FlightStatus` and surfaced in the JSON by extending `report_generator.c`, without changing `finalize_and_write_report`'s control flow.
3. **Dependency Inversion Principle (DIP)** — `report_thread` depends on the `finalize_and_write_report` abstraction; it has no knowledge of `report_generator`'s existence as a separate binary.

---

## 1. Signalling the Final Pass (AC1)

```c
/* main(), after the step loop ends */
pthread_mutex_lock(&ctx.mutex);
ctx.sim_done = true;
pthread_cond_broadcast(&ctx.cond_environment);
pthread_cond_broadcast(&ctx.cond_dispatch);
pthread_cond_broadcast(&ctx.cond_results);
pthread_cond_broadcast(&ctx.cond_violations);
pthread_mutex_unlock(&ctx.mutex);

pthread_join(thr_environment, NULL);
pthread_join(thr_dispatcher,  NULL);
pthread_join(thr_results,     NULL);
pthread_join(thr_violations,  NULL);

pthread_mutex_lock(&ctx.mutex);
ctx.report_finalize = true;
pthread_cond_signal(&ctx.cond_report);
pthread_mutex_unlock(&ctx.mutex);

pthread_join(thr_report, NULL);
```

- The four "pipeline" threads are joined **first**, in isolation — this guarantees none of them can ever set `current_thread = THREAD_REPORT` again (US107) while `report_thread` is running its finalize logic, eliminating any race between "log a late violation" and "finalize the report".
- `ctx.report_finalize = true` followed by `pthread_cond_signal(&ctx.cond_report)` reuses the **same** condition variable as US107's violation notifications — `report_thread`'s wait loop (`while (current_thread != THREAD_REPORT && !report_finalize && !sim_done)`) was specifically designed to handle both wake-up reasons (see US107).
- `pthread_join(thr_report, NULL)` blocks `main()` until `finalize_and_write_report` (including the child `report_generator` process) has fully completed — satisfying AC4's "parent waits" requirement at the Java/operator-visible level.

---

## 2. Aggregating Per-Flight Status (AC2)

```c
/* finalize_and_write_report */
for (int i = 0; i < ctx->num_flights; i++) {
    FlightStatus *fs = &ctx->shm->sim.report.flight_statuses[i];
    Flight *f = &ctx->shm->sim.flights[i];

    strncpy(fs->callsign, f->callsign, sizeof(fs->callsign) - 1);
    fs->completed_successfully = (f->current_leg >= f->num_legs) && !f->had_violation;
    fs->had_violation          = f->had_violation;
    fs->fuel_remaining          = f->current_mass - f->empty_weight - f->payload;
    fs->num_legs_completed       = f->current_leg;
}
ctx->shm->sim.report.total_flights = ctx->num_flights;
```

- `completed_successfully` combines **two** conditions: the flight reached the end of its planned route (`current_leg >= num_legs`) **and** never triggered a recorded violation (`!had_violation`) — a flight that technically finished its route but caused a near-miss earlier is *not* reported as "completed successfully".
- `fuel_remaining = current_mass - empty_weight - payload` recomputes remaining fuel from the aircraft's current total mass minus its structural and payload mass — the same formula `compute_physics_step` uses internally to detect fuel exhaustion (US107).
- `f->had_violation` is a per-flight flag set by `record_violation` (US107) the first time that flight is involved in any `ViolationEvent` — it persists for the rest of the run even if the flight subsequently resolves the conflict.

---

## 3. Overall Validation Result (AC3)

```c
ctx->shm->sim.report.total_violations_recorded;  /* accumulated throughout the run, US107 */
/* report_generator.c reads this to decide PASS/FAIL */
const char *result = (sim->report.total_violations_recorded == 0) ? "PASS" : "FAIL";
```

- `total_violations_recorded` is **not recomputed** at finalization — it is the running total accumulated by `record_violation` across every step (US107). Finalization simply reads its final value.
- The PASS/FAIL decision itself is made in `report_generator.c` (not in `simulation_engine.c`) — `finalize_and_write_report`'s job is only to ensure `sim->report` is complete and consistent (`report_ready = true`) before handing off; this keeps the "what counts as a pass" business rule in a single place (AC3, cross-referenced in **US111**'s JSON format documentation).

---

## 4. Delegating to `report_generator` (AC4)

```c
ctx->shm->sim.report.report_ready = true;

pid_t report_pid = fork();
if (report_pid == 0) {
    execl("./report_generator", "report_generator", "/aisafe_sim", NULL);
    _exit(EXIT_FAILURE);   /* execl only returns on failure */
}
waitpid(report_pid, NULL, 0);
```

- A **third kind of child process** (distinct from the per-flight children of US105): `report_generator` is a separate compiled binary, launched via `fork()` + `execl()`. `execl` replaces the child's program image entirely — it does *not* return on success.
- `report_generator` is passed `"/aisafe_sim"` as an argument — the **same shared-memory name** created in US105. It calls `shm_open("/aisafe_sim", O_RDONLY, ...)` + `mmap(..., PROT_READ, MAP_SHARED, ...)` to read `SimulationConfig` and `SimulationReport` directly — no pipe or additional IPC channel is needed, because the shared memory segment is still alive (the parent has not yet `shm_unlink`'d it).
- `waitpid(report_pid, NULL, 0)` blocks `finalize_and_write_report` (and therefore `report_thread`, and therefore `main()`'s `pthread_join(thr_report, ...)`) until the JSON file has been fully written — guaranteeing AC4: by the time `main()` proceeds to `cleanup()`/`shm_unlink`, the report file already exists on disk and is independent of the shared memory that is about to be destroyed.
- `report_ready = true` is set **before** the fork — `report_generator` checks this flag and could, in principle, refuse to run if the report were not yet finalized (defensive read-side check).

---

## 5. Releasing All Synchronization Resources (AC5)

```c
pthread_mutex_destroy(&ctx.mutex);
pthread_cond_destroy(&ctx.cond_environment);
pthread_cond_destroy(&ctx.cond_dispatch);
pthread_cond_destroy(&ctx.cond_results);
pthread_cond_destroy(&ctx.cond_violations);
pthread_cond_destroy(&ctx.cond_step_done);
pthread_cond_destroy(&ctx.cond_report);

cleanup(shm, pids, sim->num_flights);   /* US108 — releases blocked children */

for (int i = 0; i < MAX_FLIGHTS; i++) {
    char name[40];
    snprintf(name, sizeof(name), "/aisafe_go_%d", i);
    sem_close(shm->step_go[i]);   sem_unlink(name);
    snprintf(name, sizeof(name), "/aisafe_done_%d", i);
    sem_close(shm->step_done[i]); sem_unlink(name);
}
sem_close(shm->env_sem); sem_unlink("/aisafe_env");

munmap(shm, sizeof(SharedBlock));
shm_unlink("/aisafe_sim");
```

- The order matters: **threads are joined → mutex/conds destroyed → children released and reaped (`cleanup`) → named semaphores closed/unlinked → shared memory unmapped/unlinked**. Destroying `ctx.mutex` before all threads have exited (`pthread_join` already completed by this point) would be undefined behaviour; unmapping `shm` before `sem_close`/`sem_unlink` would leak named-semaphore kernel objects.
- `sem_close` releases *this process's* reference to the named semaphore; `sem_unlink` removes the `/name` from the system so the **next run** can `sem_open(..., O_CREAT | O_EXCL, ...)` cleanly (US105 already defends against a missing unlink via its own `sem_unlink` before creation, but doing it here too keeps the system clean between runs).
- This is the only point in the program where every resource allocated in US105 is symmetrically released — AC5 is satisfied by this single, centralized teardown sequence.

---

## Key Files

| File | Location | Responsibility |
|------|----------|-----------------|
| `structs.h` | `aisafe-simulation/src/main/c` | `SimulationReport`, `FlightStatus`, `SimContext.report_finalize` |
| `simulation_engine.c` | `aisafe-simulation/src/main/c` | Shutdown sequence in `main()`, `finalize_and_write_report()`, `report_thread` finalize branch, resource teardown |
| `report_generator.c` | `aisafe-simulation/src/main/c` | Reads `SimulationReport`/`SimulationConfig` from `/aisafe_sim`, computes PASS/FAIL, writes the timestamped JSON file (detailed in **US111**) |

---

## User Interaction Flow

1. The simulation reaches its configured end time (or is terminated early due to exceeding the violation threshold — US107).
2. The operator sees the program print a final summary table (violations per flight, position history) and then exit.
3. A new file `simulation_report_<timestamp>.json` appears in `aisafe-simulation/`, containing the full per-flight statuses and validation result.
4. If launched from the JavaFX `SimulationUI`, the report dialog (US111) opens automatically once the underlying process exits, reading that same file.

---

## Test Cases Coverage

| TC | What is verified |
|----|-------------------|
| TC01 | `report_thread`'s finalize branch only runs after all four pipeline threads (`environment`/`dispatcher`/`results`/`violations`) have been joined |
| TC02 | `flight_statuses[i].completed_successfully` is `false` if the flight had any recorded violation, even if it finished all legs |
| TC03 | `flight_statuses[i].fuel_remaining` matches `current_mass - empty_weight - payload` at the moment of finalization |
| TC04 | `total_flights` equals `ctx->num_flights` (all loaded flights, not just `is_active` ones) |
| TC05 | `report_generator` is launched via `fork`+`execl` with `/aisafe_sim` as its argument, and `main()` blocks via `waitpid` until it exits |
| TC06 | The JSON report file exists on disk *before* `shm_unlink("/aisafe_sim")` is called |
| TC07 | All `pthread_mutex_destroy`/`pthread_cond_destroy` calls occur after the corresponding threads have been joined |
| TC08 | All named semaphores (`step_go[i]`, `step_done[i]`, `env_sem`) are `sem_close`d and `sem_unlink`ed before program exit |

---

## Relação com outras User Stories

- **US105** — allocates the shared memory and semaphores released here.
- **US106** — `report_thread` is one of the five threads; this US covers its second wake-up path (finalize).
- **US107** — `total_violations_recorded`/`had_violation` accumulated incrementally during the run are read here in aggregate.
- **US108** — `cleanup()` (releasing blocked children) is reused as part of the shutdown sequence.
- **US111** — documents the JSON report format produced by `report_generator` and its consumption by `SimulationReportParser`/`SimulationUI` (EAPLI/operator perspective).
