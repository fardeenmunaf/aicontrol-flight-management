# aisafe-simulation

**C Simulation Engine** — multi-process flight simulation.

## Purpose

This module implements the **air control simulation engine entirely in C**, as required by the SCOMP component of the project (US100–US103). It simulates multiple flights simultaneously, detects safety violations, and outputs per-step flight state.

### Responsibilities
- Spawn one **child process per active flight plan** using `fork()`
- Simulate flight progression (position, altitude, fuel burn) in each child using a physics model
- Use **POSIX shared memory** to share flight state between processes
- Use **named POSIX semaphores** for step-by-step synchronisation between parent and children
- Use **pipes** for child → parent data communication each step
- Detect **safety violations** (proximity between aircraft, fuel exhaustion)

---

## Process Hierarchy

**Chosen model:** simulation engine (parent) → one child process per flight.

Each flight child runs its designated flight plan: it waits at the gate until its departure offset is reached, then climbs, cruises, and descends to its destination. When it lands, it sets `is_active = false` and exits.

**Why not sim → flight → leg (3-level hierarchy)?**

Legs within a single flight are strictly sequential — leg 2 can only start after leg 1 ends. There is no parallelism to gain from a separate process per leg. The 3-level model was considered and rejected on this basis.

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
  ├─ drain pipes
  ├─ check violations
  ├─ print table
  └─ advance clock → next step
```

### Startup barrier

Before the main loop begins, the parent calls `sem_wait(main_sem)` once per forked child. Each child posts to `main_sem` immediately on startup. This guarantees all children are initialised and ready before the first simulation step runs.

---

## IPC — Pipes

One pipe per flight is created before `fork()`:
- `pipe_fds[i][0]` — read end, used by the parent
- `pipe_fds[i][1]` — write end, used by child `i`

After `fork()`, each side closes the ends it does not use. The child writes a `PipeMessage` struct containing its updated position and all 25 physics values. The parent reads it after all children finish their step, and uses it to print the per-step table.

The parent closes all write ends after forking so that `read()` returns correctly when a child exits.

---

## Physics Model

Each time step computes the following in order:

### Atmosphere (ISA)
| Quantity | Formula |
|----------|---------|
| Temperature | T = 288.2 − 0.0065·h (h ≤ 11 000 m), T = 216.7 K above |
| Pressure | p = 101325·(1 − 0.0065·h/288.2)^5.2561 below tropopause; exponential above |
| Air density | ρ = p / (R·T) |
| Speed of sound | a = √(γ·R·T) |

### Speed
- **Mach**: M = √(5·((ρ₀/ρ·((1 + 0.2·(IAS/661.5)²)^3.5 − 1) + 1)^0.2857 − 1))
- **TAS**: V = M × a (m/s)

### Lift & Drag
- Cl = 2·W / (ρ·S·V²)
- Cd = Cd0 + Cl² / (π·AR·e)
- Drag = ½·ρ·V²·S·Cd

### Thrust (Mach + altitude corrected)
- T_engine = (T_stopped − λ·M) × (ρ/ρ₀)^lapse_rate
- T_total = T_engine × num_engines
- λ = (T_stopped − T_cruise) / 0.9

### Fuel Burn & Mass
- dW/dt = T_total × TSFC × g  (kg/step)
- current_mass -= dW/dt

### Phase Transitions
| Transition | Condition |
|-----------|-----------|
| Climb → Cruise | current_altitude ≥ target cruise altitude |
| Cruise → Descend | remaining horizontal distance ≤ altitude / tan(3°) |
| Cruise thrust | T_total = Drag (equilibrium) |
| Descend thrust | T_total × 0.1 |

---

## Violation Detection

After every step the parent checks:

**Proximity violation** — for every pair of active flights, computes the 3D distance using a flat-earth approximation. If the distance is below `safety_distance_threshold` (from `simulation_settings.json`), a violation is logged. Flights that landed during the current step are excluded.

**Fuel exhaustion** — if a flight's remaining fuel reaches 0 kg, a violation is logged once.

If `total_violations_detected` exceeds `max_violations_threshold`, the simulation terminates early.

---

## Simulation Parameters

Constants defined in `structs.h`:

| Constant | Value |
|----------|-------|
| `FUEL_DENSITY_KG_PER_L` | 0.804 kg/L |
| T₀ | 288.2 K |
| λ | −0.0065 K/m |
| ρ₀ | 1.225 kg/m³ |
| p₀ | 101 325 N/m² |
| R | 287 J/kg·K |
| γ | 1.4 |
| g | 9.81 m/s² |

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
make test     # runs C unit tests
make clean    # cleans build artifacts
```

---

## Structure

```
src/
├── main/c/
│   ├── simulation_engine.c   ← main() orchestrator: shared memory, semaphores, pipes, fork, step-lock loop
│   ├── flight_simulation.c   ← per-flight child process: gate hold, climb/cruise/descend, physics step
│   ├── flight_simulation.h   ← header for flight_simulation.c
│   ├── physics.c/h           ← ISA atmosphere, lift/drag/thrust computations
│   ├── json_parser.c/h       ← loads simulation_settings.json and flightplan JSONs
│   └── structs.h             ← all shared data types: Flight, SharedMemoryData, PipeMessage, etc.
└── test/c/
    └── test_physics.c        ← C unit tests for physics functions
Makefile
flightplansSim/               ← flight plan JSON files loaded at runtime
simulation_settings.json      ← runtime simulation configuration
```

---

## Requirements

- POSIX-compliant OS (Linux)
- GCC compiler
- POSIX shared memory (`-lrt`)
- POSIX math library (`-lm`)
