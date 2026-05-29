# aisafe-simulation

**C Simulation Engine** — multi-process flight simulation.

## Purpose

This module implements the **air control simulation engine entirely in C**, as required by the SCOMP component of the project (US100–US103). It simulates multiple flights simultaneously, detects safety violations, and outputs per-step flight state.

### Responsibilities
- Spawn one **child process per active flight plan** using `fork()`
- Simulate flight progression (position, altitude, fuel burn) in each child using a full ISA physics model
- Use **POSIX shared memory** to share flight state between processes
- Use **named POSIX semaphores** for step-by-step synchronisation between parent and children
- Use **pipes** for child → parent data communication each step
- Detect **safety violations** (proximity between aircraft, fuel exhaustion)
- Notify involved flight processes of violations via **POSIX signals** (US102)
- Maintain a per-flight **position history** with validation (US101)
- Support **multi-leg flights** with independent fuel, payload, and departure time per leg

---

## Process Hierarchy

**Chosen model:** simulation engine (parent) → one child process per flight.

Each flight child runs its designated flight plan: it waits at the gate until its departure offset is reached, then climbs, cruises, and descends to its destination. When it lands, it sets `is_active = false` and exits. Flights with multiple legs reset their state at each intermediate stop and depart again for the next leg.

**Why not sim → flight → leg (3-level hierarchy)?**

Legs within a single flight are strictly sequential — leg 2 can only start after leg 1 ends. There is no parallelism to gain from a separate process per leg. The 3-level model was considered and rejected on this basis.

**Scope validation before fork:** flights whose departure offset falls outside the simulation window `[0, end_time)` are marked inactive before any `fork()` call and are skipped entirely — no process is wasted on them.

---

## Synchronisation Model (Step-Lock)

The simulation advances in fixed time steps. The parent and all child processes synchronise on every step using two types of named POSIX semaphores:

### Per-flight semaphores — `step_sems[i]`

One semaphore per flight, created before `fork()` and inherited by the child. All start at 0.

- At the start of each step, the **parent posts** to `step_sems[i]` for every active flight — this unblocks each child and tells it to run its step.
- Each **child blocks** on `sem_wait(step_sems[i])` at the top of its loop, waiting for the parent's signal.

This gives the parent **selective control**: it only wakes flights that are active in the current step. Flights that have not yet departed or have already landed stay blocked.

### Shared semaphore — `main_sem`

One semaphore shared by all children, also created before `fork()`, starts at 0.

- When a child finishes its step (physics computed, pipe message written), it calls `sem_post(main_sem)`.
- The parent calls `sem_wait(main_sem)` once for every active flight it woke up — it only continues when all active children have finished.

### Full step sequence

```
Parent                          Child i
  │                               │
  ├─ sem_post(step_sems[i]) ────► unblocks
  │                               ├─ compute physics
  │                               ├─ write to pipe
  │                               └─ sem_post(main_sem) ──► parent counts
  │
  ├─ sem_wait(main_sem) × N   (blocks until all N children finish)
  ├─ drain pipes (validate + store position history)
  ├─ advance clock
  ├─ check violations + send signals
  ├─ print table
  └─ next step
```

### Startup barrier

Before the main loop begins, the parent calls `sem_wait(main_sem)` once per forked child. Each child posts to `main_sem` immediately on startup. This guarantees all children are initialised and ready before the first simulation step runs.

---

## IPC — Pipes

One pipe per flight is created before `fork()`:
- `pipe_fds[i][0]` — read end, used by the parent
- `pipe_fds[i][1]` — write end, used by child `i`

After `fork()`, each side closes the ends it does not use. The child writes a `PipeMessage` struct containing its updated position and all 25 physics values. The parent reads it after all children finish their step, validates the received position, stores it in the per-flight history, and uses the physics data to print the per-step table.

The parent closes all write ends after forking so that `read()` returns correctly when a child exits.

---

## Signal Handling (US102)

Child processes install two POSIX signal handlers using `sigaction()` before entering their step loop.

### SIGUSR1 — proximity violation notification

Sent by the parent to **each flight involved** in a proximity violation immediately after the violation is detected.

- The handler is installed with `SA_RESTART` so that a signal arriving during `sem_wait()` causes a transparent restart rather than returning `EINTR` — the step-lock loop is never interrupted.
- `sa_mask` blocks `SIGTERM` while the SIGUSR1 handler is executing, and `sigprocmask` blocks both signals inside the handler body.
- The handler calls only `write()` (async-signal-safe per POSIX.1-2008) — `printf`/`sprintf`/`strlen` are not used inside signal handlers.
- Output format: `[CALLSIGN] SIGUSR1: safety violation detected.`

### SIGTERM — early termination

Sent by the parent to **all active flights** when total violations exceed `max_violations_threshold`.

- The handler sets the `volatile sig_atomic_t g_terminate` flag (TP4 flag pattern) and writes a message via `write()`.
- `exit()` is NOT called from the handler — it is not async-signal-safe.
- The child checks `g_terminate` and `shm->termination_requested` after each `sem_wait()` and breaks the loop cleanly.

---

## Position History & Validation (US101)

The parent maintains a `PositionHistory` buffer for every flight. After reading each pipe message, it validates the received position before storing it.

### Validation (`is_valid_position`)

A position is accepted only if:
- latitude ∈ [−90, 90]
- longitude ∈ [−180, 180]
- altitude ≥ 0

Invalid updates are rejected with a warning line and do not corrupt the history buffer (TC05).

### History storage (`append_position`)

Each accepted position is appended to a per-flight `entries[]` array (max `MAX_HISTORY = 200` entries, chronological order). The buffer never overflows — appends past the limit are silently dropped.

### Seeding

The departure airport position (ground level, t = 0) is seeded into each flight's history before the main loop starts, so the full route from gate to gate is captured.

### End-of-simulation report

After the step loop, the parent prints a `POSITION HISTORY (US101)` table for every in-scope flight showing step number, latitude, longitude, and altitude for each stored entry.

---

## Multi-Leg Flights

A flight plan may contain multiple sequential legs, each with its own fuel load, payload, departure offset, and waypoint range.

### Data model

```
Flight
  └── legs[MAX_LEGS]          ← one Leg struct per leg
       ├── departure_offset_s  ← seconds from t=0 when this leg departs
       ├── fuel_mass           ← kg of fuel for this leg
       ├── payload             ← kg of passengers + cargo
       ├── waypoint_start_index← first navigation target for this leg
       └── waypoint_end_index  ← destination waypoint for this leg
```

### Leg transition sequence

When `compute_physics_step()` sets `is_active = false` (arrival detected) and a next leg exists:

1. The child increments `current_leg`.
2. It resets `departure_offset_s`, `payload`, `fuel_mass`, `current_mass`, `current_waypoint_index`, and `initial_mass` from the next leg's data.
3. Sets `leg_just_completed = true` and `is_active = true` before `sem_post` — the parent observes consistent state.
4. The parent clears `leg_just_completed` (child is blocked on `step_sem` — no concurrent write) and resets `prev_phase` to `PHASE_GATE` so the next phase transition banner correctly shows `GATE → CLIMB`.

---

## Waypoint-Based Navigation

Each flight plan contains an ordered list of waypoints (up to `MAX_WAYPOINTS = 50`). Each waypoint carries a lat/lon/alt position, a target IAS, and a `segment_phase` tag that determines the flight phase while flying toward it.

### Phase determination

The current phase is read from `waypoints[current_waypoint_index].segment_phase`. As the aircraft progresses, `advance_waypoints()` skips waypoints that have been passed: a waypoint is considered passed when the aircraft's distance to the leg destination is ≤ the waypoint's own distance to the destination.

### Cruise altitude

The target cruise altitude for a leg is taken from the waypoint at `waypoint_start_index` (the top-of-climb waypoint). This is used for altitude capping during climb and for the `CA(m)` column in the output table.

### Altitude capping

- **Climb phase**: altitude is capped at the current target waypoint's altitude to prevent overshooting the cruise level.
- **Descent phase**: altitude is floored at the destination waypoint's altitude.

---

## Spherical Navigation

All geodesic computations use spherical-Earth formulas with Earth radius R = 6 371 000 m.

| Function | Purpose |
|----------|---------|
| `haversine_m()` | Great-circle distance between two lat/lon points |
| `bearing_rad()` | Compass bearing (radians) from point A to point B |
| `advance_position()` | Moves a lat/lon point distance D along bearing B |

The **arrival check** uses `haversine_m` with `ARRIVAL_THRESHOLD_M = 2 000 m`. When the aircraft is within 2 km of the destination, it is considered landed.

**Arrival snapping**: if `horiz_dist ≥ dist_to_dest`, the aircraft snaps directly to the destination rather than overshooting. Fuel is scaled proportionally to the fraction of the step actually flown.

---

## Physics Model

Each time step computes the following in order:

### Atmosphere (ISA)

| Quantity | Formula |
|----------|---------|
| Temperature | T = 288.15 − 0.0065·h (h ≤ 11 000 m), T = 216.65 K above |
| Pressure | p = 101 325·(1 − 0.0065·h/288.15)^5.2561 below tropopause; p = 22 632·exp(−g·(h−11 000)/(R·216.65)) above |
| Air density | ρ = p / (R·T) |
| Speed of sound | a = √(γ·R·T) |

### IAS Schedule

The target indicated airspeed is phase- and altitude-dependent:

| Condition | IAS |
|-----------|-----|
| Altitude < 3 048 m (10 000 ft) | 250 kt (ATC regulation) |
| Climb above 3 048 m | 300 kt |
| Descent | 280 kt |

### Speed

- **Mach**: M = √(5·((ρ₀/ρ·((1 + 0.2·(IAS/661.5)²)^3.5 − 1) + 1)^(2/7) − 1))
- **TAS**: V = M × a (m/s)

### Aerodynamics

- Cl = 2·W / (ρ·S·V²)
- Cd = Cd0 + Cl² / (π·AR·e)
- Lift = ½·ρ·V²·S·Cl
- Drag = ½·ρ·V²·S·Cd

### Thrust (Mach + altitude corrected)

- λ = (T_stopped − T_cruise) / 0.9
- T_sea_level = T_stopped − λ·M
- T_engine = T_sea_level × (ρ/ρ₀)^lapse_rate

Phase-dependent total thrust:

| Phase | Total thrust |
|-------|-------------|
| Climb | T_engine × num_engines (full) |
| Cruise | = Drag (equilibrium — no net vertical force) |
| Descent | T_engine × num_engines × 0.1 (near-idle) |

### Step Integration

- **Climb rate**: dh/dt = (T_total − Drag) × TAS / (mass × g)
- **Fuel burn**: dW = T_total × TSFC × dt / g (kg/step)
- **Horizontal distance**: dist = TAS × cos(asin(climb_rate / TAS)) × dt
- **Mass floor**: current_mass ≥ empty_weight + payload (fuel cannot go negative)

---

## Violation Detection

After every step, the parent checks violations while children are blocked on their step semaphores (shared memory safe to read without extra synchronisation).

### Proximity violation

For every pair of active flights, the 3D distance is computed using a flat-earth approximation:
- dx = Δlon × cos(avg_lat) × R
- dy = Δlat × R
- dz = Δalt

If the distance is below `safety_distance_threshold`, a violation is logged, both flights are flagged, and the parent sends **SIGUSR1** to each involved child process.

### Fuel exhaustion

If a flight's remaining fuel (current_mass − empty_weight − payload) drops to ≤ 0, a violation is logged once per flight.

### Early termination

If `total_violations_detected > max_violations_threshold`, the parent:
1. Sends **SIGTERM** to every still-active child process.
2. Sets `shm->termination_requested = true`.
3. The main loop breaks; children exit on their next termination check.

---

## Per-Step Output

Every step produces:

1. **Step banner** — simulation time (seconds + wall-clock `[HH:MM]`), step number, active/pending/violation counts.
2. **Phase transition banners** — one line per flight that changed phase this step (e.g. `GATE → CLIMB`, `CRUISE → DESCEND`).
3. **25-column physics table** — one row per active flight:

| Column | Description |
|--------|-------------|
| CALLSGN | Flight identifier |
| PHASE | GATE / CLIMB / CRUISE / DESCEND |
| LAT, LON | Position in degrees |
| ALT(m) | Altitude in metres |
| DEST(km) | Distance to destination |
| CA(m) | Target cruise altitude |
| T(K), P(Pa), rho, a(m/s) | ISA atmosphere at current altitude |
| IAS(kt), Mach, TAS(m/s) | Speed quantities |
| CL, CD | Lift and drag coefficients |
| Lift(N), Drag(N) | Aerodynamic forces |
| Te(N), Tt(N) | Per-engine and total thrust |
| VS(m/s) | Vertical speed |
| dF(kg) | Fuel burned this step |
| dD(km) | Horizontal distance covered this step |
| M(kg) | Current total mass |
| Frem(kg) | Remaining fuel |

A **step 0** initial table is printed before any physics runs, showing each flight at its departure gate.

---

## Simulation Parameters

Constants defined in `structs.h`:

| Constant | Value |
|----------|-------|
| `FUEL_DENSITY_KG_PER_L` | 0.804 kg/L |
| T₀ | 288.15 K |
| λ | −0.0065 K/m |
| ρ₀ | 1.225 kg/m³ |
| p₀ | 101 325 N/m² |
| R | 287.05 J/kg·K |
| γ | 1.4 |
| g | 9.81 m/s² |
| `ARRIVAL_THRESHOLD_M` | 2 000 m |
| `MAX_FLIGHTS` | 100 |
| `MAX_WAYPOINTS` | 50 |
| `MAX_LEGS` | 10 |
| `MAX_HISTORY` | 200 |

Runtime parameters in `simulation_settings.json`:

| Field | Description |
|-------|-------------|
| `sim_start` | Wall-clock anchor (e.g. `2026-05-20T06:00:00`) |
| `start_time` | Simulation clock start in seconds |
| `end_time` | Simulation clock end in seconds |
| `time_step` | Seconds per step |
| `safety_distance_threshold` | Metres — proximity violation threshold |
| `max_violations_threshold` | Stop sim if total violations exceed this |

---

## Build

```bash
cd aisafe-simulation
make          # builds the simulation engine
make run      # builds and runs — output written to simulation_output.txt
make test     # runs C unit tests (physics + position tracker)
make clean    # cleans build artifacts
```

---

## Requirements

- POSIX-compliant OS (Linux)
- GCC compiler
- POSIX shared memory (`-lrt`)
- POSIX math library (`-lm`)

---

## Documentation

[Docs Folder](./docs)

---

## Libraries

- CJSON - For parsing all JSON files (flight plans, simulation settings)

---

## Team Members Evaluation

| Member | ID | US Done | Evaluation |
| --- | --- | --- | --- | 
| Fardeen Munaf | 1231567 | US103 | 20 |
| Eduardo Pacheco | 1231566 | US102 | 20 |
| Tomás Barbosa | 1241340 | US109 | 20 |
| Tiago Sousa | 1240719 | US101 | 20 |
| Diogo Remelgado | 1221407 | US100 | 20 |