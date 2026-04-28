# aisafe-simulation

**C Simulation Engine** — high-performance multi-process flight simulation.

## Purpose

This module implements the **air control simulation engine entirely in C**, as required by the SCOMP component of the project (US100–US114). It simulates multiple flights simultaneously, detects safety violations, and communicates results back to the Java application.

### Responsibilities
- Spawn one **child process per active flight plan** using `fork()`
- Simulate flight progression (position updates, altitude changes) in each child
- Use **POSIX shared memory** to share flight state between processes
- Use **semaphores** for process synchronization
- Use **signals** (`SIGSTOP`, `SIGCONT`, `SIGUSR1`) to control simulation flow
- Use **threads** (`pthreads`) for parallel safety checks within each flight process
- Detect **safety violations** (e.g., collisions, altitude breaches) and write reports

### Communication with Java
The Java side (`aisafe.app.aircontrol.console`) launches this engine as a subprocess and communicates via:
- **stdin/stdout pipes** or **shared memory segments** for status data
- Exit codes for basic pass/fail results

## Build

```bash
cd aisafe-simulation
make          # builds the simulation engine
make test     # runs C unit tests
make clean    # cleans build artifacts
```

## Structure

```
src/
├── main/c/
│   ├── simulation_engine.c   ← main() orchestrator, forks flight workers
│   ├── flight_worker.c       ← per-flight process logic
│   ├── safety_monitor.c      ← collision & violation detection (threads)
│   └── ipc_utils.c           ← shared memory, semaphore helpers
└── test/c/
    └── test_simulation.c     ← C unit tests
Makefile
```

## Requirements

- POSIX-compliant OS (Linux / macOS)
- GCC compiler
- POSIX threads (`-lpthread`)
- POSIX shared memory (`-lrt` on Linux)
