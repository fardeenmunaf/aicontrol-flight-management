# US110 — Integrate Environmental Influences into Simulation

## User Story

> As a **PO**, I want the simulation to incorporate environmental factors such as wind into the simulation, so that the flight paths become more realistic and adapt to dynamic conditions.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC1 | The parent process must spawn a dedicated **environment thread** (`pthread_create`) before the first time step begins, running concurrently alongside the safety violation detection and report generation threads; it must be properly joined (`pthread_join`) before the parent exits. |
| AC2 | The environment thread retrieves wind speed and wind direction from the system's weather service for the air control areas covered by the simulation. If unavailable, it applies a defined fallback (flight-plan defaults or calm conditions) and logs the situation. |
| AC3 | At each simulation time step the environment thread writes the current wind data to the shared memory segment under a **mutex/semaphore**, before flight processes read it. |
| AC4 | Wind speed and direction in shared memory are used by each flight process when computing aircraft movement; ground velocity is the vector sum of air velocity (TAS) and wind velocity, so results demonstrably differ under different wind conditions. |
| AC5 | The environment thread participates in the **lockstep synchronisation** (US108): it is the first phase each step, ensuring wind is written before the Dispatcher wakes child processes. |
| AC6 | Implemented in **C** using POSIX thread primitives (`pthread_create/join`, `sem_t`). Shared memory uses the same `shm_open`/`mmap` mechanism established in US105. |

---

## Implementation Overview

This US spans two modules: the Java export layer and the C simulation engine.

| Module | Role |
|--------|------|
| `aisafe.core` | `ExportWeatherDataService` — reads all `WeatherData` records from the DB via `WeatherDataRepository` and serialises them to `weather_db.json` |
| `aisafe.app.javafx` | `SimulationUI` — calls `ExportWeatherDataService.exportToJson()` before launching the C process, ensuring live DB data is written to `weather_db.json` |
| `aisafe-simulation` — `environment.h/c` | New environment thread: loads `weather_db.json` at startup, updates `env_wind[]` in shared memory at each lockstep phase |
| `aisafe-simulation` — `structs.h` | `WindData` struct, `env_wind[MAX_FLIGHTS]` + `env_sem` added to `SharedBlock`; `THREAD_ENVIRONMENT` added to `SimThreadPhase`; `cond_environment` added to `SimContext` |
| `aisafe-simulation` — `simulation_engine.c` | Spawns and joins environment thread; initialises `env_sem`; triggers `THREAD_ENVIRONMENT` as first phase each step |
| `aisafe-simulation` — `flight_simulation.c` | Flight processes read `env_wind[i]` under `env_sem`; `compute_physics_step` applies wind vector to compute ground velocity and actual track |

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

- **Ubiquitous Language**: Terms like `wind speed`, `wind direction`, `ground speed`, `track`, `TAS`, `tailwind`, and `headwind` appear consistently across the C simulation, the Java export service, and the documentation.
- **Bounded Context**: Environmental data belongs to the Simulation bounded context. The Java side exposes weather data only through the `ExportWeatherDataService` export — no simulation internals leak into the domain layer.
- **Repository Pattern**: `ExportWeatherDataService` depends on `WeatherDataRepository` (interface), decoupling the export logic from the JPA implementation. Any alternative storage can provide the same interface.
- **Separation of Concerns**: The environment thread is responsible only for retrieving and publishing wind data; flight processes are responsible only for reading and applying it. Neither crosses into the other's responsibility.

### Gang of Four (GoF) Patterns

| Pattern | Application | Benefit |
|---------|-------------|---------|
| **Observer / Blackboard** | `env_wind[]` in shared memory acts as a blackboard: the environment thread writes, all flight processes read independently | Decouples producers from consumers; adding more flight processes requires no change to the environment thread |
| **Facade** | `ExportWeatherDataService.exportToJson()` hides Jackson serialisation and repository iteration behind a single call | Callers (JavaFX UI) never interact with Jackson or JPA directly |
| **Data Transfer Object (DTO)** | The `weather_db.json` file and the `WindData` struct act as a DTO boundary between the Java domain layer and the C simulation | Java and C are fully decoupled; neither knows about the other's internals |
| **Template Method** | The lockstep pipeline — `THREAD_ENVIRONMENT → THREAD_DISPATCHER → THREAD_RESULTS → THREAD_VIOLATIONS → THREAD_REPORT → THREAD_IDLE` — defines a fixed algorithm skeleton that every step follows | Adding new step phases (e.g. a second environment source) requires only inserting a new enum value and thread |

### SOLID Principles

- **Single Responsibility Principle (SRP)**: `environment.c` has one job — fetch wind and publish it to shared memory. `flight_simulation.c` has one job — simulate aircraft physics. `ExportWeatherDataService` has one job — export weather data to JSON.
- **Open/Closed Principle (OCP)**: New weather data sources can be added inside `environment.c` (e.g. a TCP weather client) without changing any flight simulation code; new physical effects can be added to `compute_physics_step` without touching the environment thread.
- **Dependency Inversion Principle (DIP)**: `ExportWeatherDataService` depends on `WeatherDataRepository` (interface), not on `JpaWeatherDataRepository`. The in-memory stub can be swapped in for tests.
- **Interface Segregation Principle (ISP)**: The shared memory `WindData` struct exposes only `wind_speed_ms` and `wind_direction_deg` — the minimal contract needed by flight processes.

---

## Thread Lockstep Sequence

Each simulation step follows a strictly ordered pipeline. The environment thread is **always first**, guaranteeing that wind data is written before any flight process reads it.

```
Main thread (per step)
       │
       ▼
┌─────────────────────────────────────┐
│ THREAD_ENVIRONMENT                  │
│  environment_thread()               │
│  • For each active flight:          │
│    - Check weather_db.json (DB)     │
│    - Fallback: waypoint wind from   │
│      flight-plan JSON               │
│  • sem_wait(env_sem)                │
│  • Write env_wind[i]                │
│  • sem_post(env_sem)                │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ THREAD_DISPATCHER                   │
│  Posts step_go[i] for active flights│
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ Child flight processes (parallel)   │
│  sem_wait(env_sem)                  │
│  Read env_wind[flight_index]        │
│  sem_post(env_sem)                  │
│  compute_physics_step(..., wind)    │
│  sem_post(step_done[i])             │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ THREAD_RESULTS → THREAD_VIOLATIONS  │
│ → THREAD_REPORT → THREAD_IDLE       │
└─────────────────────────────────────┘
```

---

## Wind Physics Model (AC4)

Wind is applied as a **vector addition** to horizontal air velocity, giving the true ground velocity and actual track over the ground.

### Formula

```
V_air_N = horiz_TAS × cos(heading)      // northward air velocity
V_air_E = horiz_TAS × sin(heading)      // eastward air velocity

// Meteorological convention: wind_dir is the direction FROM which wind blows.
// The velocity vector therefore points TOWARD (wind_dir + 180°).
wind_to_rad = (wind_dir_deg + 180°) × π/180

V_wind_N = wind_speed × cos(wind_to_rad)
V_wind_E = wind_speed × sin(wind_to_rad)

GS_N = V_air_N + V_wind_N              // ground speed northward
GS_E = V_air_E + V_wind_E              // ground speed eastward

ground_speed = √(GS_N² + GS_E²)
track        = atan2(GS_E, GS_N)       // actual track over ground
horiz_dist   = ground_speed × Δt
```

Where `horiz_TAS = √(max(0, TAS² − climb_rate²))` is the horizontal component of True Air Speed.

### Practical Effect

| Scenario | Effect on ground track |
|----------|----------------------|
| Tailwind (wind aligned with heading) | Ground speed > TAS; earlier arrival |
| Headwind (wind opposed to heading) | Ground speed < TAS; later arrival |
| Crosswind (wind perpendicular to heading) | Aircraft drifts; track ≠ heading |

---

## Weather Data Source Priority

At each time step the environment thread resolves wind using the following priority:

```
1. weather_db.json  (exported from Java WeatherData DB records)
      │ Aircraft position (lat/lon/alt) falls within a bounding box entry
      │ → use that entry's windDirection / windSpeed
      │
      ▼  (no matching entry found)
2. Flight-plan JSON  (Segment.windDirection / Segment.windSpeed)
      │ Loaded at startup into waypoints[].wind_direction_deg
      │ and waypoints[].wind_speed_ms
      │
      ▼  (waypoint has no data)
3. Calm conditions  (0 m/s, no log — silent default)
```

### Exporting the DB to weather_db.json

Before launching the C simulation, `SimulationUI` calls:

```java
ExportWeatherDataService service = new ExportWeatherDataService(
        PersistenceContext.repositories().weatherData());
int count = service.exportToJson("aisafe-simulation/weather_db.json");
// count = number of WeatherData records exported
```

### weather_db.json Format

```json
[
  {
    "lat1": 37.0,
    "lon1": -10.0,
    "lat2": 42.0,
    "lon2": -6.0,
    "altitudeInf": 0.0,
    "altitudeSup": 12000.0,
    "windDirection": 270.0,
    "windSpeed": 12.0
  }
]
```

The C environment thread matches each aircraft against these bounding boxes at every step. The first matching entry wins.

---

## Shared Memory Layout

`SharedBlock` (in POSIX shared memory `/aisafe_sim`) was extended with:

```c
typedef struct {
    double wind_speed_ms;
    double wind_direction_deg;
} WindData;

typedef struct {
    SimulationData sim;
    sem_t          step_go[MAX_FLIGHTS];
    sem_t          step_done[MAX_FLIGHTS];
    bool           terminate;
    WindData       env_wind[MAX_FLIGHTS]; /* written by env thread, read by flight processes */
    sem_t          env_sem;               /* pshared binary semaphore (value 1) */
} SharedBlock;
```

`env_sem` is initialised with `pshared=1` so it is shared across both parent threads and child processes:

```c
sem_init(&shm->env_sem, 1, 1);
```

---

## Graceful Fallback (AC2)

If `weather_db.json` is absent, unreadable, or contains no entry covering the aircraft's current position, the environment thread:

1. Logs once at startup: `[ENV] weather_db.json not found at '...' — using flight-plan defaults.`
2. Uses the wind values already stored on the current waypoint (loaded at startup from the flight-plan JSON).
3. If waypoint wind is also zero (no data at all), calm conditions (0 m/s) are applied silently.

The simulation continues running normally in all fallback cases — no step is skipped.

---

## Key Files

| File | Location | Responsibility |
|------|----------|----------------|
| `environment.h` | `aisafe-simulation/src/main/c` | Declares `environment_thread` |
| `environment.c` | `aisafe-simulation/src/main/c` | Loads `weather_db.json`; updates `env_wind[]` each step under `env_sem`; participates in lockstep |
| `structs.h` | `aisafe-simulation/src/main/c` | `WindData`, `env_wind[]`, `env_sem` in `SharedBlock`; `THREAD_ENVIRONMENT` in `SimThreadPhase`; `cond_environment` in `SimContext` |
| `simulation_engine.c` | `aisafe-simulation/src/main/c` | Spawns and joins environment thread; initialises `env_sem`; triggers `THREAD_ENVIRONMENT` as first phase each step |
| `flight_simulation.c` | `aisafe-simulation/src/main/c` | `compute_physics_step` receives wind parameters; applies vector addition for ground velocity and track |
| `ExportWeatherDataService.java` | `aisafe.core` / `weatherData.application` | Iterates `WeatherDataRepository.findAll()`, serialises bounding boxes + wind to `weather_db.json` |
| `SimulationUI.java` | `aisafe.app.javafx` / `javafxui.presentation.simulation` | Calls `ExportWeatherDataService` before `make`; live DB data is written before the C binary starts |

---

## User Interaction Flow

### JavaFX UI

1. The ATCC opens the **Simulation** screen and clicks **Run Simulation**.
2. `SimulationUI` calls `ExportWeatherDataService.exportToJson(...)` — all `WeatherData` records from the DB are written to `weather_db.json`.
3. `make` compiles the C binary if needed.
4. `simulation_engine` starts; at step 0 the environment thread loads `weather_db.json` and logs `[ENV] Loaded N weather record(s)`.
5. Each step the environment thread logs the resolved wind per active flight: `[ENV] step N  CALLSIGN  wind X.X m/s from YYY°  (DB)` or `(plan)`.

### Console / make run

1. Run `make run` from `aisafe-simulation/`.
2. If `weather_db.json` exists in that directory, the environment thread uses it.
3. If not, the thread falls back to flight-plan JSON defaults and logs the situation.

---

## Test Cases Coverage

| TC | What is verified |
|----|-----------------|
| TC01 | Environment thread is created before step 1; `pthread_create` returns 0 |
| TC02 | Thread loads wind from `weather_db.json` when the file exists; logs record count |
| TC03 | `env_wind[i]` is updated in shared memory at each step |
| TC04 | Ground positions differ between a run with wind=12 m/s and one with wind=0 m/s |
| TC05 | `env_sem` is acquired before write (env thread) and before read (flight process); no data race |
| TC06 | `THREAD_ENVIRONMENT` fires before `THREAD_DISPATCHER` — wind data is never stale |
| TC07 | Missing `weather_db.json` → fallback applied, simulation continues, situation logged once |
| TC08 | `pthread_join(thr_environment)` returns successfully after simulation ends |
| TC09 | Other threads (violations, report) are not blocked by the env thread's semaphore section |
| TC10 | Flight process reads the exact wind values written by the env thread in the same step |
