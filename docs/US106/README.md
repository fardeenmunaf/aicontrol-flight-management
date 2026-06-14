# US106 — Implement Function-Specific Threads in the Parent Process

## User Story

> As a **Flight Control Operator**, I want the parent process to run several dedicated threads — one for environment, one for dispatching flight steps, one for collecting results, one for detecting safety violations, and one for reporting — so that each concern is handled independently while remaining synchronized step by step.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC1 | The parent process creates a thread responsible for collecting and broadcasting environment data (wind) |
| AC2 | The parent process creates a thread responsible for dispatching the per-step authorization to each flight process |
| AC3 | The parent process creates a thread responsible for collecting results from flight processes once they finish a step |
| AC4 | The parent process creates a thread responsible for detecting safety violations after results are collected |
| AC5 | The parent process creates a thread responsible for generating/logging the simulation report |
| AC6 | All threads coordinate through a shared `pthread_mutex_t` and dedicated `pthread_cond_t` condition variables, taking turns according to a well-defined phase sequence |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe-simulation` — `structs.h` | `SimContext` (mutex + 6 condition variables + `current_thread`), `SimThreadPhase` enum |
| `aisafe-simulation` — `simulation_engine.c` | `environment_thread`, `dispatcher_thread`, `results_thread`, `violations_thread`, `report_thread`, and the `main()` step loop that drives `current_thread` through the phase sequence |

---

## Architecture — The Phase Sequence (`SimThreadPhase`)

```c
typedef enum {
    THREAD_IDLE,
    THREAD_ENVIRONMENT,
    THREAD_DISPATCHER,
    THREAD_RESULTS,
    THREAD_VIOLATIONS,
    THREAD_REPORT      /* only entered when new violations are detected */
} SimThreadPhase;
```

For every simulation step, `main()` drives `ctx.current_thread` through:

```
THREAD_IDLE
   │  main() sets THREAD_ENVIRONMENT, signals cond_environment
   ▼
THREAD_ENVIRONMENT  (US110 — recompute/broadcast wind, env_sem)
   │  environment_thread sets THREAD_DISPATCHER, signals cond_dispatch
   ▼
THREAD_DISPATCHER   (AC2 — sem_post(step_go[i]) for each active flight)
   │  dispatcher_thread sets THREAD_RESULTS, signals cond_results
   ▼
THREAD_RESULTS      (AC3 — sem_wait(step_done[i]), collect positions)
   │  results_thread sets THREAD_VIOLATIONS, signals cond_violations
   ▼
THREAD_VIOLATIONS   (AC4 — check_violations(), print step table)
   │
   ├─ no new violations ──► THREAD_IDLE, signal cond_step_done ──► main() continues
   │
   └─ new violations ─────► THREAD_REPORT, signal cond_report
                                  │
                                  ▼
                          THREAD_REPORT (AC5 — log each ViolationEvent)
                                  │  sets THREAD_IDLE, signals cond_step_done
                                  ▼
                              main() continues
```

Each thread spends most of its life blocked on `pthread_cond_wait`, woken only when `ctx.current_thread` matches the phase it owns — this is a classic **turn-taking / rendez-vous** pattern implemented with one mutex and several condition variables (AC6).

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

- Each thread is a **Domain Service** with a single, narrowly-scoped responsibility (dispatch, collect, validate, report) operating on the shared `SimulationData` — none of them owns state beyond what it needs for its phase.
- `SimContext` acts as a **Specification/Coordinator** object: it does not contain simulation domain data itself (that's in `SharedBlock.sim`), only the *synchronization* state needed to sequence the domain services.

### Gang of Four (GoF) Patterns

| Pattern | Application | Benefit |
|---------|-------------|---------|
| **Chain of Responsibility** | The `THREAD_ENVIRONMENT → THREAD_DISPATCHER → THREAD_RESULTS → THREAD_VIOLATIONS → (THREAD_REPORT) → THREAD_IDLE` sequence — each thread, on finishing its work, decides the *next* phase and hands off | Each thread is decoupled from the others; a thread only needs to know which phase comes after its own |
| **Observer** (via condition variables) | Every thread "observes" `ctx.current_thread` through `pthread_cond_wait` on its own condition variable, woken only when relevant | Threads sleep (zero CPU) instead of busy-polling a shared flag |
| **State** | `SimThreadPhase` enum represents the explicit state of the "simulation step state machine"; each thread's behaviour is conditional on this state | Adding `THREAD_REPORT` as a conditional extra state (US107) required no change to threads that don't participate in it |

### SOLID Principles

1. **Single Responsibility Principle (SRP)** — `dispatcher_thread` only posts `step_go`; `results_thread` only collects `step_done` + positions; `violations_thread` only runs `check_violations`; `report_thread` only logs/serializes.
2. **Open/Closed Principle (OCP)** — the optional `THREAD_REPORT` branch (US107) was added by extending the enum and `violations_thread`'s branching logic, without modifying `dispatcher_thread` or `results_thread` at all.
3. **Interface Segregation Principle (ISP)** — each thread function takes the same `SimContext *` but reads/writes only the subset of fields relevant to its phase (e.g., `dispatcher_thread` never touches `pending_violations`).
4. **Dependency Inversion Principle (DIP)** — all five threads depend only on `SimContext`/`SharedBlock` (the shared abstraction defined in `structs.h`), never directly on each other's implementation.

---

## 1. Dispatcher Thread (AC2)

```c
void *dispatcher_thread(void *arg) {
    SimContext *ctx = arg;
    pthread_mutex_lock(&ctx->mutex);
    while (!ctx->sim_done) {
        while (ctx->current_thread != THREAD_DISPATCHER && !ctx->sim_done)
            pthread_cond_wait(&ctx->cond_dispatch, &ctx->mutex);
        if (ctx->sim_done) break;

        for (int i = 0; i < ctx->num_flights; i++)
            if (ctx->was_active[i])
                sem_post(SEM_GO(ctx->shm, i));

        ctx->current_thread = THREAD_RESULTS;
        pthread_cond_signal(&ctx->cond_results);
    }
    pthread_mutex_unlock(&ctx->mutex);
    return NULL;
}
```

- Holds `ctx->mutex` while waiting on `cond_dispatch` — `pthread_cond_wait` atomically releases the mutex while blocked and re-acquires it on wake-up, so no other thread is starved.
- `ctx->was_active[i]` (computed by `main()` at the start of the step, from each flight's `is_active` flag *before* this step's physics) determines which flights receive a `step_go` post — a flight that lands or is terminated mid-step is excluded from the *next* dispatch.
- `sem_post(SEM_GO(shm, i))` is the only cross-process synchronization primitive this thread touches — it never blocks (a semaphore post never blocks), so the thread quickly hands off to `THREAD_RESULTS`.

---

## 2. Results Thread (AC3)

```c
void *results_thread(void *arg) {
    SimContext *ctx = arg;
    pthread_mutex_lock(&ctx->mutex);
    while (!ctx->sim_done) {
        while (ctx->current_thread != THREAD_RESULTS && !ctx->sim_done)
            pthread_cond_wait(&ctx->cond_results, &ctx->mutex);
        if (ctx->sim_done) break;

        pthread_mutex_unlock(&ctx->mutex);  /* release before blocking on sem_wait */
        for (int i = 0; i < ctx->num_flights; i++) {
            if (!ctx->was_active[i]) continue;
            sem_wait(SEM_DONE(ctx->shm, i));
            append_position(&ctx->histories[i], &ctx->shm->sim.flights[i]);
        }
        pthread_mutex_lock(&ctx->mutex);

        ctx->current_thread = THREAD_VIOLATIONS;
        pthread_cond_signal(&ctx->cond_violations);
    }
    pthread_mutex_unlock(&ctx->mutex);
    return NULL;
}
```

- This is the **critical handoff** between threads and child processes (AC3): `sem_wait(SEM_DONE(shm, i))` blocks until child `i` finishes computing its physics step and posts `step_done[i]` (see US108).
- The thread **releases `ctx->mutex` before the blocking `sem_wait` loop** — holding a `pthread_mutex_t` across a long blocking call on an unrelated synchronization primitive (a named semaphore) would prevent `main()`/other threads from ever observing `ctx->current_thread` changes, risking deadlock. The mutex is re-acquired only once all children have reported.
- `append_position(&ctx->histories[i], ...)` records the flight's new position/altitude/fuel into `PositionHistory` — used later both for the step-by-step table (`print_step_table`) and the final report (US109).

---

## 3. Violations Thread (AC4) — see US107 for the report hand-off

```c
void *violations_thread(void *arg) {
    SimContext *ctx = arg;
    pthread_mutex_lock(&ctx->mutex);
    while (!ctx->sim_done) {
        while (ctx->current_thread != THREAD_VIOLATIONS && !ctx->sim_done)
            pthread_cond_wait(&ctx->cond_violations, &ctx->mutex);
        if (ctx->sim_done) break;

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
    }
    pthread_mutex_unlock(&ctx->mutex);
    return NULL;
}
```

- `check_violations()` is the heaviest computation in the pipeline: pairwise collision/proximity checks, fuel-exhaustion detection and reroute lookahead for every active flight pair.
- `ctx->shm->sim.current_time += ctx->step_time` advances the simulation clock **only after** this step's results and violation checks are complete — guaranteeing that anything printed or logged for "time T" reflects a fully-resolved state at T.
- The `if (new_violations > 0)` branch is the conditional fork in the phase chain (AC5/US107): normally the pipeline returns straight to `THREAD_IDLE`, but a new violation routes through `THREAD_REPORT` first so it gets logged immediately.

---

## 4. Report Thread (AC5)

```c
void *report_thread(void *arg) {
    SimContext *ctx = arg;
    pthread_mutex_lock(&ctx->mutex);
    while (1) {
        while (ctx->current_thread != THREAD_REPORT && !ctx->report_finalize && !ctx->sim_done)
            pthread_cond_wait(&ctx->cond_report, &ctx->mutex);

        if (ctx->report_finalize) {
            finalize_and_write_report(ctx->shm, ctx);   /* US109 */
            break;
        }
        if (ctx->sim_done) break;

        while (ctx->logged_violation_idx < ctx->shm->sim.report.total_violations_recorded) {
            ViolationEvent *v = &ctx->shm->sim.report.violations[ctx->logged_violation_idx];
            /* printf(...) — see US107 for the full log format */
            ctx->logged_violation_idx++;
        }
        ctx->pending_violations = 0;

        ctx->current_thread = THREAD_IDLE;
        pthread_cond_signal(&ctx->cond_step_done);
    }
    pthread_mutex_unlock(&ctx->mutex);
    return NULL;
}
```

- This thread has **two distinct wake-up reasons**, both signalled via `cond_report`: (1) `current_thread == THREAD_REPORT` during the normal step loop (US107), and (2) `report_finalize == true` at simulation shutdown (US109) — the `while` condition checks both so the thread doesn't miss either.
- `ctx->logged_violation_idx` is a **cursor** into `shm->sim.report.violations[]`, ensuring each `ViolationEvent` is logged exactly once even if multiple violations accumulate in `pending_violations` across steps.

---

## 5. The Step Loop in `main()` (AC6 — orchestration)

```c
while (sim->current_time < sim->config.end_time) {
    for (int i = 0; i < ctx.num_flights; i++)
        ctx.was_active[i] = sim->flights[i].is_active;

    pthread_mutex_lock(&ctx.mutex);
    ctx.step_num++;
    ctx.step_time = sim->config.time_step;
    ctx.current_thread = THREAD_ENVIRONMENT;
    pthread_cond_signal(&ctx.cond_environment);

    while (ctx.current_thread != THREAD_IDLE)
        pthread_cond_wait(&ctx.cond_step_done, &ctx.mutex);

    pthread_mutex_unlock(&ctx.mutex);

    if (termination_requested(sim)) break;
}
```

- `main()` itself behaves like a sixth participant in the rendez-vous: it sets the *first* phase (`THREAD_ENVIRONMENT`) and then blocks on `cond_step_done` until the entire chain has cycled back to `THREAD_IDLE` — i.e., until **every** thread for this step has run exactly once (or, if violations occurred, the report thread too).
- Because all five worker threads loop "wait for my phase → do work → set next phase → signal next thread", `main()` never needs to know about `THREAD_REPORT` specifically — it just waits for `THREAD_IDLE`, regardless of which path got there.

---

## Key Files

| File | Location | Responsibility |
|------|----------|-----------------|
| `structs.h` | `aisafe-simulation/src/main/c` | `SimContext`, `SimThreadPhase` enum, `SEM_GO`/`SEM_DONE` macros |
| `simulation_engine.c` | `aisafe-simulation/src/main/c` | All five thread functions + the `main()` step loop that drives `current_thread` |

---

## User Interaction Flow

This US has no direct UI — it is purely an internal coordination mechanism observed indirectly:

1. The Flight Control Operator starts a simulation (US105).
2. For every simulated time step, the operator sees one row printed by `print_step_table` (called from `THREAD_VIOLATIONS`) — the visible result of the five-thread phase cycle completing once.
3. If a violation is detected, an additional `[REPORT]` line (from `THREAD_REPORT`) appears immediately after the step row, before the next step's row — demonstrating the conditional `THREAD_REPORT` branch.

---

## Test Cases Coverage

| TC | What is verified |
|----|-------------------|
| TC01 | `environment_thread` runs first each step and transitions to `THREAD_DISPATCHER` |
| TC02 | `dispatcher_thread` posts `step_go[i]` only for flights where `was_active[i]` is true |
| TC03 | `results_thread` releases `ctx->mutex` before blocking on `sem_wait`, allowing other threads to observe `current_thread` |
| TC04 | `violations_thread` advances `sim->current_time` only after `check_violations()` completes |
| TC05 | When `new_violations == 0`, the phase goes directly `THREAD_VIOLATIONS → THREAD_IDLE` (report thread is not woken) |
| TC06 | When `new_violations > 0`, the phase goes `THREAD_VIOLATIONS → THREAD_REPORT → THREAD_IDLE` |
| TC07 | `main()`'s `cond_step_done` wait correctly unblocks only when `current_thread == THREAD_IDLE`, regardless of whether `THREAD_REPORT` ran |
| TC08 | At shutdown, all four condition-variable loops (`environment`/`dispatch`/`results`/`violations`) exit via `sim_done`, and `report_thread` exits via `report_finalize` |

---

## Relação com outras User Stories

- **US105** — creates these five threads and the `SimContext` they share.
- **US107** — details the `THREAD_VIOLATIONS → THREAD_REPORT` hand-off introduced here.
- **US108** — details the `step_go`/`step_done` semaphore protocol used by `dispatcher_thread`/`results_thread`.
- **US109** — details `report_thread`'s `report_finalize` path.
- **US110** — details `environment_thread`.
