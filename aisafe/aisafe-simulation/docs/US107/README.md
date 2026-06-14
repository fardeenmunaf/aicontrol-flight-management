# US107 — Notify Report Thread via Condition Variables upon Safety Violation Detection

## User Story

> As a **Flight Control Operator**, I want the violations thread to immediately notify the report thread, using condition variables, whenever a safety violation is detected during the simulation, so that the violation is logged as soon as it happens rather than only at the end of the run.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC1 | The violations thread continuously monitors the shared simulation state for newly detected safety violations |
| AC2 | Upon detecting one or more new violations, the violations thread signals the report thread using a dedicated condition variable |
| AC3 | The report thread, normally blocked waiting on that condition variable, wakes up immediately and processes/logs the new violation(s) |
| AC4 | Access to the shared violation data is protected by a mutex, avoiding race conditions between the two threads |
| AC5 | After logging, the report thread hands control back to the normal step pipeline (`THREAD_IDLE`) so the simulation can continue |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe-simulation` — `structs.h` | `SimContext.cond_report`, `SimContext.pending_violations`, `SimContext.logged_violation_idx`, `SimulationReport.violations[]`, `ViolationEvent` |
| `aisafe-simulation` — `simulation_engine.c` | `check_violations()` (populates `report.violations[]`), `violations_thread` (AC1/AC2), `report_thread` (AC3/AC5) |

---

## Architecture — The Violation Notification Hand-off

```
violations_thread                                  report_thread
──────────────────                                  ─────────────
THREAD_VIOLATIONS:
  new = check_violations(shm, ctx)   ─── AC1 ───►   (blocked on cond_report,
  shm->sim.report.violations[] +=                     current_thread != THREAD_REPORT)
     new ViolationEvent entries
  shm->sim.report.total_violations_recorded += new

  if (new > 0):
      ctx->pending_violations += new
      ctx->current_thread = THREAD_REPORT  ── AC2 ──►  wakes up (AC3)
      pthread_cond_signal(&ctx->cond_report)            current_thread == THREAD_REPORT

                                                    while (logged_violation_idx <
                                                           report.total_violations_recorded):
                                                        log ViolationEvent[logged_violation_idx]
                                                        logged_violation_idx++
                                                    pending_violations = 0

                                                    ctx->current_thread = THREAD_IDLE  ── AC5
                                                    pthread_cond_signal(&ctx->cond_step_done)
  else:
      ctx->current_thread = THREAD_IDLE
      pthread_cond_signal(&ctx->cond_step_done)
```

All of the above happens **while `ctx->mutex` is held** by whichever thread is running — `pthread_cond_wait`/`pthread_cond_signal` are always used in this mutex-protected pattern (AC4).

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

- A `ViolationEvent` is a **Domain Event**: an immutable record of "something happened at simulation time T involving aircraft A (and possibly B)". `check_violations()` is the only place that creates these events; `report_thread` only *reacts* to them.
- The `pending_violations` counter and `logged_violation_idx` cursor implement an **at-least-once / exactly-once delivery guarantee** for domain events between the detecting thread and the logging thread — no event can be both skipped and double-logged because the cursor only ever increases and is compared against `total_violations_recorded`, which only ever increases.

### Gang of Four (GoF) Patterns

| Pattern | Application | Benefit |
|---------|-------------|---------|
| **Observer** | `report_thread` "observes" the `THREAD_REPORT` phase via `pthread_cond_wait(&ctx->cond_report, ...)`; `violations_thread` is the "subject" that notifies it | The detecting thread does not need to know *how* violations are logged — it only flips a phase flag and signals |
| **Command / Event Queue** (informal) | `shm->sim.report.violations[]` acts as an append-only event log; `logged_violation_idx` is the "read pointer" of a single-consumer queue | Multiple violations detected in the same step are all queued and processed in order, none lost |
| **Mediator** | `SimContext` (mutex + `cond_report` + phase enum) mediates between `violations_thread` and `report_thread` so they never reference each other directly | Threads remain independently testable/replaceable |

### SOLID Principles

1. **Single Responsibility Principle (SRP)** — `violations_thread` detects and records events; `report_thread` only consumes and logs them. Detection logic never duplicates logging logic.
2. **Open/Closed Principle (OCP)** — adding a new violation *type* (e.g., airspace incursion) only requires `check_violations()` to append a new `ViolationEvent` with an appropriate description; `report_thread`'s consume loop is unchanged.
3. **Interface Segregation Principle (ISP)** — `report_thread` only reads `pending_violations`, `logged_violation_idx`, and `report.violations[]`/`total_violations_recorded` — it has no access to (and no need for) the collision-detection internals of `check_violations()`.
4. **Dependency Inversion Principle (DIP)** — both threads depend on the `SimContext`/`ViolationEvent` abstractions in `structs.h`, not on each other's function bodies.

---

## 1. Producing Violation Events (AC1) — `check_violations()`

```c
int check_violations(SharedBlock *shm, SimContext *ctx) {
    SimulationData *sim = &shm->sim;
    int new_violations = 0;

    for (int i = 0; i < ctx->num_flights; i++) {
        for (int j = i + 1; j < ctx->num_flights; j++) {
            if (!ctx->was_active[i] || !ctx->was_active[j]) continue;

            double sep = distance_meters(&sim->flights[i].current_pos,
                                          &sim->flights[j].current_pos);
            if (sep < sim->config.min_separation_m) {
                record_violation(sim, ctx, i, j, sep);   /* appends ViolationEvent */
                new_violations++;
            }
            /* ... proximity warning (not a recorded violation) ... */
        }

        /* fuel exhaustion: single-flight event, flight_a_id == flight_b_id */
        if (sim->flights[i].fuel_remaining <= 0 && !ctx->prev_phase[i] /* ... */) {
            record_violation(sim, ctx, i, i, 0.0);
            new_violations++;
        }
    }
    return new_violations;
}
```

- `record_violation(sim, ctx, a, b, sep)` writes a new `ViolationEvent` at index `sim->report.total_violations_recorded` of `sim->report.violations[]` (bounded by `MAX_RECORDED_VIOLATIONS`), capturing both flights' positions/velocities, the simulation timestamp, wall-clock time, and `sep`. It then increments `sim->report.total_violations_recorded`.
- A **fuel-exhaustion event** is recorded with `flight_a_id == flight_b_id` — `report_thread`'s logging code uses this equality to distinguish a single-flight event ("Flight X ran out of fuel") from a two-flight separation violation ("Flights X and Y violated minimum separation").
- This function returns *only the count of violations newly added during this call* (`new_violations`), not the running total — `violations_thread` uses this count to decide whether to enter `THREAD_REPORT` (AC1/AC2).

---

## 2. Signalling the Report Thread (AC2) — inside `violations_thread`

```c
int new_violations = check_violations(ctx->shm, ctx);
ctx->shm->sim.current_time += ctx->step_time;
print_step_table(ctx->shm, ctx);

if (new_violations > 0) {
    ctx->pending_violations += new_violations;
    ctx->current_thread = THREAD_REPORT;
    pthread_cond_signal(&ctx->cond_report);
} else {
    ctx->current_thread = THREAD_IDLE;
    pthread_cond_signal(&ctx->cond_step_done);
}
```

- `pthread_cond_signal` (not `broadcast`) is used because **exactly one** thread (`report_thread`) is ever waiting on `cond_report` — a targeted wake-up is sufficient and slightly cheaper than a broadcast.
- The signal is sent **while `ctx->mutex` is still held** by `violations_thread` (AC4). `report_thread`, woken from `pthread_cond_wait`, will block re-acquiring the mutex until `violations_thread` releases it at the top of its next loop iteration — there is no window where both threads read/write `pending_violations` or `report.violations[]` concurrently.
- `ctx->pending_violations += new_violations` is **cumulative**: if `report_thread` is somehow delayed and another step also produces violations before it runs (not possible in the current strictly-sequential phase machine, but the field is designed defensively), the counter would still reflect the true backlog.

---

## 3. Report Thread Wake-up and Logging (AC3)

```c
while (ctx->current_thread != THREAD_REPORT && !ctx->report_finalize && !ctx->sim_done)
    pthread_cond_wait(&ctx->cond_report, &ctx->mutex);

if (ctx->report_finalize) { finalize_and_write_report(ctx->shm, ctx); break; } /* US109 */
if (ctx->sim_done) break;

while (ctx->logged_violation_idx < ctx->shm->sim.report.total_violations_recorded) {
    ViolationEvent *v = &ctx->shm->sim.report.violations[ctx->logged_violation_idx];

    if (v->flight_a_id == v->flight_b_id) {
        printf("[REPORT] t=%.1f — Flight %s (id %d) ran out of fuel at (%.4f, %.4f)\n",
               v->timestamp, v->callsign_a, v->flight_a_id,
               v->pos_a.latitude, v->pos_a.longitude);
    } else {
        printf("[REPORT] t=%.1f — Separation violation between %s and %s: %.1f m "
               "(min %.1f m) at (%.4f, %.4f) / (%.4f, %.4f)\n",
               v->timestamp, v->callsign_a, v->callsign_b, v->separation_m,
               ctx->shm->sim.config.min_separation_m,
               v->pos_a.latitude, v->pos_a.longitude,
               v->pos_b.latitude, v->pos_b.longitude);
    }
    ctx->logged_violation_idx++;
}
ctx->pending_violations = 0;
```

- The `while (ctx->current_thread != THREAD_REPORT && !ctx->report_finalize && !ctx->sim_done)` guard handles **all three** ways this thread can legitimately be woken (normal violation, end-of-simulation finalize, or early shutdown) — this is the textbook reason a condition-variable wait must always be in a `while` loop, not an `if`: a spurious wake-up or a wake-up for a *different* reason must re-check the condition (AC3).
- The inner `while (ctx->logged_violation_idx < ... total_violations_recorded)` drains **all** newly recorded events in one go — even though `violations_thread` currently only ever adds events one step at a time, this loop correctly handles the case where `check_violations()` recorded multiple violations (e.g., two separate near-misses) in the same step.
- `pending_violations = 0` resets the "new since last report" counter once everything has been logged — `logged_violation_idx` (not `pending_violations`) is the authoritative cursor; `pending_violations` exists mainly as a quick non-zero/zero signal for diagnostics.

---

## 4. Returning Control to the Pipeline (AC5)

```c
ctx->current_thread = THREAD_IDLE;
pthread_cond_signal(&ctx->cond_step_done);
```

- This is **identical** to the `else` branch in `violations_thread` for the no-violation case — from `main()`'s perspective (which is blocked in `while (ctx.current_thread != THREAD_IDLE) pthread_cond_wait(&ctx.cond_step_done, ...)`), the violation-report detour is transparent: the step either took the short path (`VIOLATIONS → IDLE`) or the long path (`VIOLATIONS → REPORT → IDLE`), but `main()`'s wake-up condition and the loop structure are exactly the same either way.
- Because the violation is logged **within the same simulated step** in which it was detected — before `main()` proceeds to the next step — the operator sees the `[REPORT]` line immediately after the step's table row, satisfying the "as soon as it happens" requirement of the user story.

---

## Key Files

| File | Location | Responsibility |
|------|----------|-----------------|
| `structs.h` | `aisafe-simulation/src/main/c` | `SimContext` fields (`cond_report`, `pending_violations`, `logged_violation_idx`, `report_finalize`), `ViolationEvent`, `SimulationReport.violations[]` |
| `simulation_engine.c` | `aisafe-simulation/src/main/c` | `check_violations()` / `record_violation()` (AC1), `violations_thread` (AC2), `report_thread` (AC3–AC5) |

---

## User Interaction Flow

1. The simulation is running normally; the operator sees one step-table row per simulated time step.
2. At some step, `check_violations()` detects that two aircraft have violated the minimum separation distance (or a flight has run out of fuel).
3. Immediately following that step's table row, a `[REPORT]` line appears describing the violation (callsigns, positions, separation distance or fuel-exhaustion message).
4. The simulation continues to the next step without interruption — the violation does not pause or abort the run (unless the cumulative violation count exceeds `max_violations_threshold`, which triggers termination independently).

---

## Test Cases Coverage

| TC | What is verified |
|----|-------------------|
| TC01 | `check_violations()` returns the count of *newly* recorded violations for the current step only |
| TC02 | A separation violation appends a `ViolationEvent` with `flight_a_id != flight_b_id` and a non-zero `separation_m` |
| TC03 | A fuel-exhaustion event appends a `ViolationEvent` with `flight_a_id == flight_b_id` |
| TC04 | When `new_violations == 0`, `report_thread` is never woken (`current_thread` goes straight to `THREAD_IDLE`) |
| TC05 | When `new_violations > 0`, `report_thread` wakes, logs exactly the new entries (`logged_violation_idx` advances by `new_violations`), and resets `pending_violations` to 0 |
| TC06 | `report_thread`'s wait loop correctly distinguishes a `THREAD_REPORT` wake-up from a `report_finalize` wake-up from a `sim_done` wake-up |
| TC07 | All reads/writes to `report.violations[]`, `total_violations_recorded`, `pending_violations`, and `logged_violation_idx` occur while `ctx->mutex` is held |
| TC08 | After logging, `current_thread == THREAD_IDLE` and `main()`'s `cond_step_done` wait unblocks, allowing the simulation to proceed to the next step |

---

## Relação com outras User Stories

- **US105** — initializes `cond_report` and the `SimContext` used here.
- **US106** — describes the overall thread/phase architecture; this US details the `THREAD_VIOLATIONS → THREAD_REPORT` branch specifically.
- **US108** — the per-step lockstep semaphore protocol that precedes violation detection each step.
- **US109** — `report_finalize`/`finalize_and_write_report()`, the *other* reason `report_thread` wakes up, reusing the same `cond_report`.
- **US111** — the EAPLI/operator-facing documentation of the final JSON report, which includes the same `violations[]` accumulated here.
