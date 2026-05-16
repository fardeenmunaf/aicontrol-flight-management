# aisafe-simulation

**C Simulation Engine** — high-performance multi-process flight simulation.

## Purpose

This module implements the **air control simulation engine entirely in C**, as required by the SCOMP component of the project (US100–US114). It simulates multiple flights simultaneously, detects safety violations, and communicates results back to the Java application.

### Responsibilities
- Spawn one **child process per active flight plan** using `fork()`
- Simulate flight progression (position, altitude, fuel burn) in each child using a physics model
- Use **POSIX shared memory** to share flight state between processes
- Use **semaphores** for step-by-step process synchronisation
- Use **signals** (`SIGSTOP`, `SIGCONT`, `SIGUSR1`) to control simulation flow
- Use **threads** (`pthreads`) for parallel safety checks within each flight process
- Detect **safety violations** (e.g., collisions, altitude breaches) and write reports

### Communication with Java
The Java side (`aisafe.app.aircontrol.console`) launches this engine as a subprocess and communicates via:
- **stdin/stdout pipes** or **shared memory segments** for status data
- Exit codes for basic pass/fail results

---

## Process Hierarchy

**Chosen model:** simulation engine (parent) → one child process per flight.

Each flight child runs through all of its legs sequentially in a loop. When a leg ends (aircraft lands), the child updates `current_mass`, `initial_fuel_mass`, `payload`, and resets the waypoint index, then continues with the next leg.

**Why not sim → flight → leg (3-level hierarchy)?**

Legs within a single flight are strictly sequential: leg 2 can only start after leg 1 ends (the aircraft lands, refuels, and departs again). There is no parallelism to gain from a separate process per leg. Spawning a child per leg would add fork/wait overhead and extra IPC complexity for work that is naturally handled as a loop inside the flight process. The 3-level model was considered and rejected on this basis.

**Synchronisation model (step-lock):**

The main process and child processes synchronise on every 60-second time step using two semaphore types defined in `SharedMemoryData`:
- `sem_t step_semaphores[MAX_FLIGHTS]` — the main process posts to each flight's semaphore to signal "advance one step"
- `sem_t main_semaphore` — each child posts here when its step is complete; the main process waits for all children before checking violations and advancing the simulation clock

---

## Physics Model

Each 60-second time step computes the following in order:

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
- λ = (T_stopped − T_cruise) / 0.9  *(derived from sea-level stopped and Mach-0.9 values)*

### Fuel Burn & Mass
- dW/dt = T_total × TSFC × g  (kg/step)
- current_mass -= dW/dt

### Phase Transitions (automatic, no extra process)
| Transition | Condition |
|-----------|-----------|
| Climb → Cruise | current_altitude ≥ target cruise altitude |
| Cruise → Descend | remaining horizontal distance ≤ altitude / tan(3°) |
| Cruise thrust | T_total = Drag (equilibrium) |
| Descend thrust | T_total × 0.1 |

---

## Simulation Parameters

Constants defined in `structs.h`:

| Constant | Value | Source |
|----------|-------|--------|
| `FUEL_DENSITY_KG_PER_L` | 0.804 kg/L | Spec: 50 000 L Jet-A → 40 200 kg |
| T₀ | 288.2 K | ISA reference temperature |
| λ | −0.0065 K/m | ISA temperature lapse rate |
| ρ₀ | 1.225 kg/m³ | ISA sea-level air density |
| p₀ | 101 325 N/m² | ISA sea-level pressure |
| R | 287 J/kg·K | Specific gas constant for dry air |
| γ | 1.4 | Ratio of specific heats |
| g | 9.81 m/s² | Standard gravity |
| Time step | 60 s | Simulation spec |

---

## Build

```bash
cd aisafe-simulation
make          # builds the simulation engine
make test     # runs C unit tests
make clean    # cleans build artifacts
```

---

## Structure

```
src/
├── main/c/
│   ├── simulation_engine.c   ← main() orchestrator, forks flight children, step-lock loop
│   ├── flight_simulation.c   ← per-flight child process logic (to be implemented)
│   ├── physics.c/h           ← ISA atmosphere, lift/drag/thrust computations
│   ├── json_parser.c/h       ← loads simulation_settings.json and flightplan JSONs
│   └── structs.h             ← all shared data types and IPC structures
└── test/c/
    └── test_physics.c        ← C unit tests for physics functions
Makefile
```

Files still to be created as implementation proceeds:
- `flight_simulation.c` — per-flight child process loop (legs, phase transitions, physics step)
- `safety_monitor.c` — collision and violation detection (pthreads)
- `ipc_utils.c` — shared memory init/cleanup helpers

---

## Requirements

- POSIX-compliant OS (Linux / macOS)
- GCC compiler
- POSIX threads (`-lpthread`)
- POSIX shared memory (`-lrt` on Linux)
