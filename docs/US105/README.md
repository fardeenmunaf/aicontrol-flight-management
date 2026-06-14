# US105 — Initialize Hybrid Simulation Environment with Shared Memory

## User Story

> As a **Flight Control Operator**, I want to start the simulation with a multi-threaded parent process and multiple child flight processes communicating through a shared memory area, so that the system efficiently coordinates simulation data across processes.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC1 | The parent process spawns dedicated threads for its functionalities |
| AC2 | Each flight is launched as an independent process |
| AC3 | A shared memory segment is allocated and properly initialized for inter-process communication |
| AC4 | Flight processes are configured to use semaphores for synchronization |
| AC5 | This component must be implemented in C and must utilize threads, mutexes, condition variables, and signals |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe-simulation` — `structs.h` | Defines `SharedBlock` (the POSIX shared-memory layout), `SimulationData`, `SimContext` (parent-thread coordination state) |
| `aisafe-simulation` — `simulation_engine.c` | `main()` — allocates/initializes shared memory, named semaphores, forks one process per flight, creates the five parent threads |
| `aisafe-simulation` — `flight_simulation.c` | `run_flight_process()` — child entry point; registers signal handlers, signals readiness, waits on its semaphore |
| `aisafe-simulation` — `json_parser.c` | `load_simulation_settings()`, `load_all_flight_plans()` — populate `SimulationData` before the fork loop |

---

## Architecture

```
                       ┌──────────────────────────────────────────────┐
                       │  PARENT PROCESS (simulation_engine.c, main)   │
                       │                                                │
                       │  1. shm_open + ftruncate + mmap  → SharedBlock│
                       │  2. sem_open × (2×MAX_FLIGHTS + 1)            │
                       │  3. load_simulation_settings / flight plans   │
                       │  4. fork() × num_forked   ───────────────────┼──┐
                       │  5. startup barrier: sem_wait(step_done[i])   │  │
                       │  6. pthread_create × 5 (env/dispatcher/       │  │
                       │     results/violations/report)                │  │
                       └──────────────────────────────────────────────┘  │
                                          ▲                               │
                                          │ shared memory (/aisafe_sim)   │
                       ┌──────────────────┴───────────────┐              │
                       │  CHILD i (flight_simulation.c)    │ ◄────────────┘
                       │  run_flight_process(shm, i)       │
                       │  - install SIGUSR1/SIGTERM        │
                       │  - sem_post(step_done[i])  (ready)│
                       │  - loop: sem_wait(step_go[i]) ...  │
                       └────────────────────────────────────┘
```

---

## EAPLI Theoretical Principles — Practical Application

> Although this US is implemented entirely in C (SCOMP), the same architectural principles taught in EAPLI apply at the systems level.

### Domain-Driven Design (DDD)

- **Bounded Context**: the simulation is its own bounded context — `SharedBlock` is the explicit contract (the "shared kernel") between the parent process and every flight process; no other part of the system reaches into this memory.
- **Aggregate-like consistency boundary**: `SimulationData` inside `SharedBlock` is the single consistent snapshot of the simulation state at any instant; all readers/writers (parent threads and child processes) agree on its layout via the shared `structs.h` header.

### Gang of Four (GoF) Patterns

| Pattern | Application | Benefit |
|---------|-------------|---------|
| **Builder** (informal) | `main()` incrementally builds the running simulation: allocate shared memory → load config → load flight plans → fork children → start threads | Each initialization step can fail independently and abort cleanly (`EXIT_FAILURE`) before later steps run |
| **Template Method** | `run_flight_process()` defines a fixed child lifecycle (install handlers → signal ready → loop on `step_go`/`step_done`) that every flight process follows identically | Adding a new flight type only requires populating `Flight`/`FlightPlan`, not changing the lifecycle |
| **Object Pool** (informal) | The fixed-size arrays `flights[MAX_FLIGHTS]`, `step_go[MAX_FLIGHTS]`, `step_done[MAX_FLIGHTS]` pre-allocate all per-flight resources inside `SharedBlock` at startup | Avoids dynamic allocation across process boundaries — shared memory has a fixed size known up front |

### SOLID Principles

1. **Single Responsibility Principle (SRP)** — `structs.h` only declares the shared layout; `json_parser.c` only loads configuration/flight plans; `simulation_engine.c::main` only orchestrates startup; `flight_simulation.c::run_flight_process` only runs one flight's lifecycle.
2. **Open/Closed Principle (OCP)** — new parent threads (e.g., the environment thread added by US110) are added by extending `SimThreadPhase` and calling one more `pthread_create`, without changing the shared-memory initialization code.
3. **Interface Segregation Principle (ISP)** — the `SEM_GO`/`SEM_DONE`/`SEM_ENV` macros expose only the semaphore operations each side needs, hiding the underlying `sem_t *` array indexing.
4. **Dependency Inversion Principle (DIP)** — both parent and child depend only on the `structs.h` contract (`SharedBlock` layout + macros), never on each other's source files.

---

## 1. Shared Memory Allocation (AC3)

```c
shm_unlink("/aisafe_sim");                 /* clean up a previous crashed run */

int shm_fd = shm_open("/aisafe_sim", O_CREAT | O_RDWR, 0600);
if (shm_fd < 0) { perror("shm_open"); return EXIT_FAILURE; }
if (ftruncate(shm_fd, sizeof(SharedBlock)) < 0) { perror("ftruncate"); return EXIT_FAILURE; }

SharedBlock *shm = mmap(NULL, sizeof(SharedBlock),
                        PROT_READ | PROT_WRITE, MAP_SHARED, shm_fd, 0);
close(shm_fd);
if (shm == MAP_FAILED) { perror("mmap"); return EXIT_FAILURE; }

memset(shm, 0, sizeof(SharedBlock));
```

- `shm_open(..., O_CREAT | O_RDWR, 0600)` creates (or opens) a POSIX shared-memory object identified by the name `/aisafe_sim`, with read/write access for the owner only.
- `ftruncate` sets its size to exactly `sizeof(SharedBlock)` — large enough to hold the entire `SimulationData`, semaphore pointers, and wind/report data.
- `mmap(..., MAP_SHARED, ...)` maps that object into the parent's address space. **After `fork()`, every child inherits this exact mapping**, so reads/writes to `*shm` are visible across process boundaries without any further IPC call.
- `memset(shm, 0, sizeof(SharedBlock))` guarantees a clean slate — important because `shm_open` may return a stale segment left over from a previous crashed run (hence the `shm_unlink` first).
- `close(shm_fd)` is safe immediately after `mmap`: the mapping keeps the underlying object alive; the file descriptor itself is no longer needed.

---

## 2. Semaphore Initialization (AC4)

```c
for (int i = 0; i < MAX_FLIGHTS; i++) {
    char name[40];
    snprintf(name, sizeof(name), "/aisafe_go_%d", i);
    sem_unlink(name);
    shm->step_go[i] = sem_open(name, O_CREAT | O_EXCL, 0600, 0);

    snprintf(name, sizeof(name), "/aisafe_done_%d", i);
    sem_unlink(name);
    shm->step_done[i] = sem_open(name, O_CREAT | O_EXCL, 0600, 0);
}
sem_unlink("/aisafe_env");
shm->env_sem = sem_open("/aisafe_env", O_CREAT | O_EXCL, 0600, 1);
```

- `SharedBlock.step_go[i]` / `step_done[i]` are **named semaphores** (`sem_t *`, created with `sem_open`), not `sem_init`-based unnamed semaphores. The project comment in `structs.h` explains why: named semaphores are inherited correctly across `fork()` on every platform, whereas process-shared `sem_init(..., pshared=1, ...)` is unsupported on macOS.
- Each flight slot `i` gets **two** semaphores:
  - `step_go[i]` — initial value `0`. The parent `sem_post`s it to authorize flight `i` to compute its next step.
  - `step_done[i]` — initial value `0`. The child `sem_post`s it once it has finished a step (and once at startup, to signal readiness).
- `env_sem` — initial value `1` (binary semaphore / mutex). Protects `env_wind[]`, written by the environment thread and read by every flight process (US110).
- `sem_unlink` before each `sem_open(..., O_CREAT | O_EXCL, ...)` removes any leftover semaphore with the same name from a previous run — `O_EXCL` would otherwise fail if the name already existed.
- `memset(shm->env_wind, 0, sizeof(shm->env_wind))` zero-initializes the wind table before the environment thread starts populating it.

---

## 3. Loading Configuration and Flight Plans

```c
SimulationData *sim = &shm->sim;

if (!load_simulation_settings("simulation_settings.json", &sim->config))
    return EXIT_FAILURE;          /* invalid duration, time step, thresholds, ... */

if (!load_all_flight_plans("flightplansSim", sim))
    return EXIT_FAILURE;          /* a flight plan failed validation */
```

`load_simulation_settings` and `load_all_flight_plans` (in `json_parser.c`) populate `sim->config` (`SimulationConfig`: time window, time step, safety/collision thresholds, reroute flag, wall-clock anchor) and `sim->flights[]`/`sim->num_flights` directly inside the shared-memory block — there is no separate "load into local struct, then copy" step, since `sim` is already a pointer into `shm`.

After loading, `sim->report` is seeded (`sim_start_time`, `total_flights`, `wall_clock_start = time(NULL)`), and `run_flight_selection_menu(sim)` lets the operator choose which loaded flights to actually simulate (toggling `flights[i].is_active`).

---

## 4. Forking One Process Per Flight (AC2)

```c
pid_t pids[MAX_FLIGHTS];
memset(pids, 0, sizeof(pids));
sim->current_time = sim->config.start_time;

for (int i = 0; i < sim->num_flights; i++) {
    sim->flights[i].process_id = 0;
    if (!sim->flights[i].is_active) continue;

    fflush(stdout);
    pid_t pid = fork();
    if (pid < 0) { perror("fork failed"); cleanup(shm, pids, sim->num_flights); return EXIT_FAILURE; }

    if (pid == 0) {
        run_flight_process(shm, i);   /* child: never returns */
        exit(EXIT_SUCCESS);
    }

    pids[i] = pid;
    sim->flights[i].process_id = pid;
}
```

- Only flights selected via the flight-selection menu (`is_active == true`) are forked — out-of-scope or deselected flights have no child process and no PID.
- `fflush(stdout)` before each `fork()` avoids duplicated buffered output being flushed independently by both parent and child.
- In the child (`pid == 0`), `run_flight_process(shm, i)` (in `flight_simulation.c`) takes over and never returns to `main()`'s loop — it `exit()`s directly.
- The parent records each child's PID in both the local `pids[]` array (used for `waitpid`/`kill`) and `sim->flights[i].process_id` (visible in shared memory, e.g., for reporting).

---

## 5. Child Startup — `run_flight_process` (AC4/AC5)

```c
void run_flight_process(SharedBlock *shm, int flight_index) {
    install_signal_handlers();             /* SIGUSR1, SIGTERM — see US102 */

    sem_post(SEM_DONE(shm, flight_index)); /* "I'm ready" — reuses step_done */

    while (1) {
        int r;
        do {
            r = sem_wait(SEM_GO(shm, flight_index));
        } while (r < 0 && errno == EINTR && !g_terminate);

        if (r < 0 || g_terminate || shm->terminate || !flight->is_active)
            break;

        compute_physics_step(...);          /* writes new position/mass into shm */

        sem_post(SEM_DONE(shm, flight_index));

        if (!flight->is_active) break;
    }
}
```

- The very first thing each child does is `sem_post(step_done[i])` — this is the **startup barrier signal**, reused from the same semaphore used later for step completion (no separate "ready" semaphore is needed).
- The child then immediately blocks on `sem_wait(step_go[i])`, guaranteeing it cannot start computing physics before the parent explicitly authorizes the first step.
- The `do/while` around `sem_wait` retries on `EINTR` (the wait is interrupted by a delivered signal such as `SIGUSR1`) unless a termination flag is set — see US102 for the signal-handling details.

---

## 6. Startup Barrier and `SimContext` Initialization (AC1/AC5)

```c
/* Startup barrier: wait for all children to post step_done once */
for (int i = 0; i < sim->num_flights; i++)
    if (pids[i] > 0)
        sem_wait(SEM_DONE(shm, i));

for (int i = 0; i < sim->num_flights; i++)
    if (pids[i] > 0)
        sim->prev_pos[i] = sim->flights[i].current_pos;

SimContext ctx;
memset(&ctx, 0, sizeof(ctx));
ctx.shm = shm; ctx.num_flights = sim->num_flights; ...
pthread_mutex_init(&ctx.mutex, NULL);
pthread_cond_init(&ctx.cond_environment, NULL);
pthread_cond_init(&ctx.cond_dispatch,   NULL);
pthread_cond_init(&ctx.cond_results,    NULL);
pthread_cond_init(&ctx.cond_violations, NULL);
pthread_cond_init(&ctx.cond_step_done,  NULL);
pthread_cond_init(&ctx.cond_report,     NULL);
ctx.current_thread = THREAD_IDLE;

pthread_create(&thr_environment, NULL, environment_thread, &ctx);
pthread_create(&thr_dispatcher,  NULL, dispatcher_thread,  &ctx);
pthread_create(&thr_results,     NULL, results_thread,     &ctx);
pthread_create(&thr_violations,  NULL, violations_thread,  &ctx);
pthread_create(&thr_report,      NULL, report_thread,      &ctx);
```

- The **startup barrier** ensures the parent does not begin the step loop until every forked child has registered its signal handlers and is blocked on `step_go` — without it, the Dispatcher could `sem_post(step_go[i])` before child `i` even calls `sem_wait`, which would still work correctly with semaphores (the post is not lost) but the barrier also guarantees `prev_pos[i]` is initialized from a position the child has actually written.
- `SimContext` (AC1/AC5) is the **single coordination object** shared by all five parent threads: one `pthread_mutex_t`, six `pthread_cond_t` variables (one per phase transition plus `cond_step_done`), and the `current_thread` phase enum (`SimThreadPhase`) that the threads use to take turns — see **US106** for how each thread uses this context, and **US108** for the `step_go`/`step_done` semaphore protocol with the children.
- All five threads (`environment_thread`, `dispatcher_thread`, `results_thread`, `violations_thread`, `report_thread`) are created here, satisfying AC1 ("the parent process spawns dedicated threads for its functionalities").

---

## Key Files

| File | Location | Responsibility |
|------|----------|-----------------|
| `structs.h` | `aisafe-simulation/src/main/c` | `SharedBlock`, `SimulationData`, `SimContext`, `SimThreadPhase`, `SEM_GO`/`SEM_DONE`/`SEM_ENV` macros |
| `simulation_engine.c` | `aisafe-simulation/src/main/c` | `main()` — shared memory + semaphore setup, config/flight-plan loading, fork loop, startup barrier, thread creation |
| `flight_simulation.c` / `.h` | `aisafe-simulation/src/main/c` | `run_flight_process()` — child entry point and per-step loop |
| `json_parser.c` / `.h` | `aisafe-simulation/src/main/c` | `load_simulation_settings`, `load_all_flight_plans`, `load_weather_db` |

---

## User Interaction Flow

1. The Flight Control Operator launches the simulation binary (directly via `make run`, or from the JavaFX `SimulationUI` — see US110).
2. `main()` allocates and zeroes the shared-memory block, creates all named semaphores, and loads `simulation_settings.json` and the flight plans from `flightplansSim/`.
3. The operator is shown the **flight selection menu** and chooses which loaded flights to include.
4. The engine prints `"N flight(s) selected — forking N child processes."`, then forks one process per selected flight.
5. The parent waits at the startup barrier until every child has signalled readiness, then creates the five worker threads and enters the main step loop (see US106/US108).

---

## Test Cases Coverage

| TC | What is verified |
|----|-------------------|
| TC01 | `shm_open`/`ftruncate`/`mmap` succeed and `SharedBlock` is zero-initialized |
| TC02 | All `2 × MAX_FLIGHTS + 1` named semaphores are created with the expected initial values (`step_go`=0, `step_done`=0, `env_sem`=1) |
| TC03 | A leftover shared-memory segment or semaphore name from a crashed run is `unlink`ed before re-creation |
| TC04 | One child process is forked per `is_active` flight, and `process_id` is recorded in shared memory |
| TC05 | The startup barrier blocks `main()` until every forked child has posted `step_done` once |
| TC06 | All five parent threads (`environment`, `dispatcher`, `results`, `violations`, `report`) are created and reach `THREAD_IDLE`/wait state before step 1 |
| TC07 | `fork()` failure mid-loop triggers `cleanup()` and `EXIT_FAILURE` without leaking already-created children |

---

## Relação com outras User Stories

- **US106** — the five threads created here implement their function-specific responsibilities.
- **US107** — uses `cond_report`/`pending_violations` initialized here.
- **US108** — the `step_go`/`step_done` semaphore protocol initialized here is the lockstep mechanism.
- **US109** — `sim->report` is seeded here and finalized at shutdown.
- **US110** — the environment thread and `env_sem`/`env_wind` initialized here.
