# US108 — Enforce Step-by-Step Simulation Synchronization

## User Story

> As a **Flight Control Operator**, I want the simulation engine to synchronize the simulation's step-by-step progression using semaphores, so that all flight processes and parent threads advance in lockstep through each simulation time step.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC1 | Semaphores are used to control the progression of each simulation time step |
| AC2 | No flight process computes step *N+1* before the parent has authorized it |
| AC3 | The parent does not advance to step *N+1* until every active flight process has finished step *N* |
| AC4 | A slower flight process never causes the simulation to silently desynchronize — the parent always waits for it |
| AC5 | The mechanism works across process boundaries (parent ↔ children), not just between threads |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe-simulation` — `structs.h` | `SharedBlock.step_go[MAX_FLIGHTS]`, `SharedBlock.step_done[MAX_FLIGHTS]` (named `sem_t *`), `SEM_GO`/`SEM_DONE` macros |
| `aisafe-simulation` — `simulation_engine.c` | Semaphore creation (`main`), `dispatcher_thread` (posts `step_go`), `results_thread` (waits on `step_done`), `cleanup()` |
| `aisafe-simulation` — `flight_simulation.c` | `run_flight_process()` — child-side `sem_wait(step_go)` / `sem_post(step_done)` loop |

---

## Architecture

```
┌────────────────────────────────────────────────────────────────┐
│  PARENT PROCESS (simulation_engine.c)                           │
│                                                                   │
│  main loop ── sets THREAD_DISPATCHER, waits on cond_step_done    │
│       │                                                          │
│  Dispatcher thread ── for each active flight i:                  │
│       │                  sem_post(SEM_GO(shm, i))   ── AC1/AC2   │
│       │                  → THREAD_RESULTS                        │
│       │                                                          │
│  Results thread ── for each active flight i:                     │
│                        sem_wait(SEM_DONE(shm, i))   ── AC1/AC3   │
│                        → THREAD_VIOLATIONS                       │
│                                                                   │
│         ↕ POSIX shared memory (/aisafe_sim) + named semaphores  │
├──────────┬──────────┬──────────┬──────────┬─────────────────────┤
│ CHILD 0  │ CHILD 1  │ CHILD 2  │ CHILD 3  │  ...  (one per flight)│
│  sem_wait(step_go[i]) → compute_physics_step() → sem_post(step_done[i])│
└──────────┴──────────┴──────────┴──────────┴─────────────────────┘
```

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

- The **simulation step** is the system's fundamental unit of consistency: no flight's state is considered "current" until *every* active flight has completed the same step. The semaphore pair `step_go`/`step_done` is the technical enforcement of this invariant — conceptually equivalent to a domain rule that an aggregate's invariants must hold at every transaction boundary.
- Each child process is a **bounded execution context** for one `Flight` — it only ever reads/writes its own `sim->flights[i]` slot plus the shared `env_wind[]` (US110), never another flight's data, preventing cross-flight corruption even without explicit per-flight locks.

### Gang of Four (GoF) Patterns

| Pattern | Application | Benefit |
|---------|-------------|---------|
| **Barrier / Rendez-vous** (synchronization pattern) | The pair `sem_wait(step_done[i])` in `results_thread` for every `i` forms a synchronization barrier — the parent cannot proceed until all participants arrive | Guarantees lockstep (AC3/AC4) regardless of relative process speeds |
| **Template Method** | Every child runs the identical loop `sem_wait(step_go) → compute_physics_step() → sem_post(step_done)`, defined once in `run_flight_process` | New flight behaviours only change what happens *inside* `compute_physics_step`, never the synchronization skeleton |
| **Proxy** (the `SEM_GO`/`SEM_DONE` macros) | `SEM_GO(shm, i)` / `SEM_DONE(shm, i)` hide the fact that semaphores are stored as `sem_t *` pointers (obtained via `sem_open`) rather than inline `sem_t` values | Code that posts/waits never needs to know whether the semaphore is named or anonymous |

### SOLID Principles

1. **Single Responsibility Principle (SRP)** — `dispatcher_thread` only *grants permission* (posts `step_go`); `results_thread` only *waits for completion* (waits on `step_done`); neither thread computes physics.
2. **Open/Closed Principle (OCP)** — the number of flights (`MAX_FLIGHTS` slots, `num_flights` active) can change without altering the synchronization protocol itself — it is parametrized by `i`.
3. **Liskov Substitution Principle (LSP)** — every child process is interchangeable from the parent's point of view: the parent's `sem_post`/`sem_wait` calls do not differ based on which flight `i` represents.
4. **Dependency Inversion Principle (DIP)** — both parent and child depend on the `SEM_GO`/`SEM_DONE` macro abstraction defined in `structs.h`, not on the underlying `sem_open` mechanics directly.

---

## 1. The Semaphore Pair (AC1, AC5)

```c
/* structs.h */
typedef struct {
    SimulationData sim;
    sem_t *step_go[MAX_FLIGHTS];     /* parent posts → child wakes  */
    sem_t *step_done[MAX_FLIGHTS];   /* child posts  → parent wakes */
    bool   terminate;
    WindData env_wind[MAX_FLIGHTS];  /* US110 */
    sem_t *env_sem;                  /* US110 */
} SharedBlock;

#define SEM_GO(shm, i)   ((shm)->step_go[i])
#define SEM_DONE(shm, i) ((shm)->step_done[i])
```

A POSIX semaphore is a non-negative integer with two atomic operations:

- **`sem_wait(sem)`** — decrements the value; if it is `0`, blocks until another process/thread increments it.
- **`sem_post(sem)`** — increments the value and wakes one process/thread blocked in `sem_wait`.

Both `step_go[i]` and `step_done[i]` are created with initial value `0` — so the very first `sem_wait` on either always blocks until the corresponding `sem_post` arrives (AC2/AC3).

---

## 2. Why Named Semaphores Instead of `sem_init`

```c
char name[40];
snprintf(name, sizeof(name), "/aisafe_go_%d", i);
sem_unlink(name);
shm->step_go[i] = sem_open(name, O_CREAT | O_EXCL, 0600, 0);
```

`sem_init(sem_t *sem, int pshared, unsigned int value)` with `pshared = 1` is the "textbook" way to create a semaphore shared between processes via `mmap`'d memory — and it works on Linux. **It is not supported on macOS** (`sem_init` with `pshared=1` returns `ENOSYS`). The project instead uses **named semaphores**:

- `sem_open(name, O_CREAT | O_EXCL, 0600, value)` creates a kernel-managed semaphore identified by a `/name`, independent of where it is mapped — every process that calls `sem_open` with the same name gets a handle to the *same* semaphore.
- `SharedBlock.step_go[i]` stores the **pointer** returned by `sem_open` (a `sem_t *`), not the semaphore itself. After `fork()`, the child inherits this pointer value, and because named semaphores are identified by name (not by address), the child's copy of the pointer refers to the same kernel object as the parent's — `sem_wait`/`sem_post` work correctly across the process boundary (AC5).
- `sem_unlink(name)` before `sem_open(..., O_CREAT | O_EXCL, ...)` removes any semaphore left over from a crashed previous run; `O_EXCL` would otherwise fail with `EEXIST`.

This satisfies AC1 and AC5 identically to the `sem_init(pshared=1)` approach, while remaining portable to macOS — the platform used for development.

---

## 3. Startup Barrier (AC2 precondition)

```c
for (int i = 0; i < sim->num_flights; i++)
    if (pids[i] > 0)
        sem_wait(SEM_DONE(shm, i));
```

Each freshly-forked child immediately does `sem_post(SEM_DONE(shm, i))` as its very first action (reusing `step_done` rather than introducing a third "ready" semaphore — see `flight_simulation.c` below). The parent waits for **all** of these posts before creating the worker threads or entering the step loop — guaranteeing every child has installed its signal handlers and is blocked on `step_go[i]` *before* the Dispatcher's first `sem_post`.

---

## 4. Dispatcher Thread — Authorizing a Step (AC1, AC2)

```c
for (int i = 0; i < ctx->num_flights; i++)
    if (ctx->was_active[i])
        sem_post(SEM_GO(ctx->shm, i));

ctx->current_thread = THREAD_RESULTS;
pthread_cond_signal(&ctx->cond_results);
```

- Each `sem_post(SEM_GO(shm, i))` increments `step_go[i]` from `0` to `1`, unblocking child `i`'s `sem_wait(SEM_GO(shm, i))`. Because semaphores are themselves atomic, the dispatcher does **not** need to hold `ctx->mutex` while issuing these posts — the mutex only protects `SimContext`'s phase-coordination fields (`current_thread`, the condition variables), not the cross-process semaphores.
- Only flights where `ctx->was_active[i]` is true receive a post — a flight that has landed, crashed, or not yet departed is simply never woken for this step, and its child process remains blocked on `sem_wait(SEM_GO(shm, i))` indefinitely (until `cleanup()` at shutdown — see below).

---

## 5. Child Process — Waiting for Authorization (AC2, AC4)

```c
/* flight_simulation.c — run_flight_process */
sem_post(SEM_DONE(shm, flight_index));      /* "ready" — startup barrier */

while (1) {
    int r;
    do {
        r = sem_wait(SEM_GO(shm, flight_index));
    } while (r < 0 && errno == EINTR && !g_terminate);

    if (r < 0 || g_terminate || shm->terminate || !flight->is_active)
        break;

    compute_physics_step(&shm->sim, flight_index, &step);   /* writes to shm */

    sem_post(SEM_DONE(shm, flight_index));

    if (!flight->is_active) break;
}
```

- `sem_wait(SEM_GO(...))` is the embodiment of **AC2**: the child literally cannot execute `compute_physics_step` until the value of `step_go[flight_index]` becomes positive, which only the Dispatcher thread can do.
- The `do { } while (r < 0 && errno == EINTR && !g_terminate)` retry handles signal delivery (e.g., `SIGUSR1` for a mid-flight reroute, US102): a signal can cause `sem_wait` to return `-1`/`EINTR` *without* having decremented the semaphore — retrying is required so the wait is not silently lost (which would desynchronize this child from the rest — violating AC4).
- After computing the step, `sem_post(SEM_DONE(shm, flight_index))` is the child's half of **AC3** — it tells the parent "I am done with step N".

---

## 6. Results Thread — The Barrier (AC3, AC4)

```c
for (int i = 0; i < ctx->num_flights; i++) {
    if (!ctx->was_active[i]) continue;
    sem_wait(SEM_DONE(ctx->shm, i));
    append_position(&ctx->histories[i], &ctx->shm->sim.flights[i]);
}
ctx->current_thread = THREAD_VIOLATIONS;
pthread_cond_signal(&ctx->cond_violations);
```

This loop is where **AC3 and AC4** are actually enforced:

- It iterates over every flight that was dispatched this step (`was_active[i]`) and calls `sem_wait(SEM_DONE(shm, i))` — which blocks until *that specific* child posts.
- The loop is sequential (`i = 0, 1, 2, ...`), but because every dispatched child is already running concurrently (it was unblocked by the Dispatcher moments earlier), the *order* of the waits does not matter — by the time the loop reaches child `k`, child `k` has likely already posted (or will shortly). The **slowest** child determines how long this loop takes overall — exactly the "lockstep" guarantee: the parent (and therefore every other thread/process) effectively waits for the slowest flight before the step is considered complete (AC4).
- `append_position` is only called *after* `sem_wait` succeeds — guaranteeing the position read from `shm->sim.flights[i]` is the value the child wrote during *this* step, not a stale value from the previous one.

---

## 7. Shutdown — Releasing Blocked Children (AC2/AC5 edge case)

```c
static void cleanup(SharedBlock *shm, pid_t *pids, int n) {
    shm->terminate = true;
    for (int i = 0; i < n; i++)
        if (pids[i] > 0)
            sem_post(SEM_GO(shm, i));      /* unblock any child still waiting */

    for (int i = 0; i < n; i++)
        if (pids[i] > 0)
            waitpid(pids[i], NULL, 0);
}
```

A flight whose `is_active` became `false` mid-simulation (landed, crashed, fuel-exhausted) is **never dispatched again** — its child sits forever in `sem_wait(SEM_GO(shm, i))`. At shutdown, `shm->terminate = true` is set first, then every such child is given one final `sem_post(SEM_GO(shm, i))` so it wakes up, observes `shm->terminate == true`, and exits cleanly — without this, `waitpid` would hang forever on that PID.

At normal exit, all `step_go`/`step_done`/`env_sem` named semaphores are released via `sem_close` + `sem_unlink` (see US105), and `/aisafe_sim` shared memory is `munmap`'d and `shm_unlink`'d.

---

## Key Files

| File | Location | Responsibility |
|------|----------|-----------------|
| `structs.h` | `aisafe-simulation/src/main/c` | `SharedBlock.step_go`/`step_done` declarations, `SEM_GO`/`SEM_DONE` macros |
| `simulation_engine.c` | `aisafe-simulation/src/main/c` | Semaphore creation (`main`), `dispatcher_thread`, `results_thread`, `cleanup()` |
| `flight_simulation.c` / `.h` | `aisafe-simulation/src/main/c` | `run_flight_process()` — child-side wait/compute/post loop |
| `docs/US108/SCOMP.md` | `docs/US108` | Supplementary narrative walkthrough (some details superseded — see note above) |

---

## User Interaction Flow

This US is purely internal coordination, observed indirectly:

1. The operator starts a simulation (US105) with several flights.
2. For each printed step row, the operator implicitly sees the result of a completed lockstep cycle — every active flight's position for that step is consistent with the same simulated time.
3. If one flight has heavier computation for a step (e.g., a reroute calculation), the operator may notice a brief pause before that step's row is printed — this is the Results thread's `sem_wait` loop waiting for that flight's `step_done`, demonstrating AC4 in practice.

---

## Test Cases Coverage

| TC | What is verified |
|----|-------------------|
| TC01 | `step_go[i]`/`step_done[i]` are created with initial value `0` for all `MAX_FLIGHTS` slots |
| TC02 | A child's `sem_wait(SEM_GO(shm, i))` blocks until the Dispatcher's `sem_post` for that specific `i` |
| TC03 | The Results thread's `sem_wait(SEM_DONE(shm, i))` blocks until child `i` posts, regardless of the order other children post |
| TC04 | A flight with `was_active[i] == false` is not dispatched and its child is not waited on this step |
| TC05 | `EINTR` from `sem_wait(SEM_GO)` (signal delivery) does not desynchronize the child — the retry loop re-attempts the wait |
| TC06 | At shutdown, every child still blocked on `step_go` is released via `cleanup()`'s final `sem_post`, and `waitpid` returns for all PIDs |
| TC07 | Named semaphores created with `sem_open(..., O_CREAT|O_EXCL, ...)` after `sem_unlink` succeed even if a previous run crashed without cleanup |

---

## Relação com outras User Stories

- **US105** — creates the named semaphores documented here.
- **US106** — places `dispatcher_thread`/`results_thread` within the broader five-thread phase sequence.
- **US102** (Sprint 2) — `SIGUSR1`/`g_terminate` and the `EINTR` retry loop in the child's `sem_wait`.
- **US110** — `env_sem`, the analogous semaphore used to protect `env_wind[]`.
