# US111 — Generate a Simulation Report

## User Story

> As a **Flight Control Operator**, I want to receive a summary of the simulation results so that I can determine if the programmed flights are safe to run.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC01 | The system generates a report and stores it in a file at the end of a simulation |
| AC02 | The report includes the total number of flights and their execution status |
| AC03 | If safety violations occur, the report lists the timestamp and position of each violation |
| AC04 | The report indicates whether the scheduled flight plans **passed** or **failed** validation |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe.aisafe-simulation` (C) | `report_generator` — standalone binary, forked by `simulation_engine` at the end of the run, writes `simulation_report_<timestamp>.json` |
| `aisafe.app.javafx` | `SimulationReportParser` (reads the newest JSON report file), `SimulationUI.showReportDialog()` (presents it to the Flight Control Operator) |
| `aisafe.core` | `simulationReport.domain.SimulationReport` — JPA aggregate for a persisted summary (totals + pass/fail), `SimulationReportRepository` |
| `aisafe.persistence.impl` | `JpaSimulationReportRepository`, `InMemorySimulationReportRepository` |

---

## Architecture

```
[simulation_engine.c]
   THREAD_REPORT phase
        │  fork() + execl("./report_generator", "report_generator", "/aisafe_sim")
        ▼
[report_generator.c]
   reads SimulationConfig + SimulationReport from POSIX shared memory ("/aisafe_sim")
        │
        ▼
   simulation_report_YYYYMMDD_HHMMSS.json   (AC01)
        │
        ▼
[SimulationReportParser]  (JavaFX, aisafe.app.javafx)
   finds the newest simulation_report_*.json in aisafe-simulation/
        │
        ▼
[SimulationUI.showReportDialog()]  — presents validation result, totals and violations table
```

This corresponds to the `THREAD_REPORT` phase of the lockstep simulation pipeline described in `docs/US110`.

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

- The report is a **read-model / DTO** of the simulation run: `report_generator.c` builds it directly from the `SimulationConfig`/`SimulationReport` C structs in shared memory, without re-deriving domain rules.
- `simulationReport.domain.SimulationReport` is a small **Aggregate Root** (`totalFlights`, `validationResult`, `generatedAt`, `simulationId`) that mirrors the JSON file's summary section for persistence/querying inside the Java domain — see *Persistence Status* below.

### Gang of Four (GoF) Patterns

| Pattern | Location | Purpose |
|---------|----------|---------|
| **Facade** | `SimulationReportParser.findAndParse()` | Hides file lookup + JSON parsing behind a single call returning an `Optional<Report>` |
| **DTO** | `SimulationReportParser.Report` / `ViolationEvent` (Java records) | Decouples the JavaFX presentation from the raw JSON produced by the C engine |
| **Repository** | `SimulationReportRepository` | Abstracts persistence of the summary `SimulationReport` aggregate |

### SOLID Principles

1. **Single Responsibility Principle (SRP)** — `report_generator` only serializes shared-memory data to JSON; `SimulationReportParser` only parses that JSON; `SimulationUI.showReportDialog()` only renders it.
2. **Open/Closed Principle (OCP)** — new sections can be added to the JSON report (and to `SimulationReportParser.Report`) without changing how `simulation_engine` forks `report_generator`.
3. **Dependency Inversion Principle (DIP)** — the Java domain depends on `SimulationReportRepository` (abstraction), with JPA and in-memory implementations provided by `aisafe.persistence.impl`.

---

## Report File Format (`simulation_report_YYYYMMDD_HHMMSS.json`)

Produced by `generate_json_report` in `report_generator.c`:

```jsonc
{
  "reportVersion": "1.0",
  "reportType": "SimulationReport",
  "reportGeneratedAt": "...",

  "simulation": {
    "totalFlights": 2,                 // AC02
    "startTime": 0.0,
    "endTime": 1234.5,
    "durationSeconds": 1234.5,
    "wallClockStart": "..."
  },

  "safetyValidation": {
    "totalViolationsDetected": 0,
    "validationResult": "PASS",        // AC04 — "PASS" or "FAIL"
    "maxViolationsThreshold": 0
  },

  "flightExecutionStatuses": [          // AC02 — per-flight execution status
    {
      "flightId": 0,
      "callsign": "TP123",
      "completed": true,
      "hadViolation": false,
      "fuelRemainingKg": 1234.5,
      "legsCompleted": 3
    }
  ],

  "violationEvents": [                  // AC03 — only if violations occurred
    {
      "violationId": 0,
      "timestamp": 120.0,
      "wallClockTime": "2026-06-14T10:32:00",
      "aircraftA": { "flightId": 0, "callsign": "TP123",
                      "position": { "latitude": 0.0, "longitude": 0.0, "altitudeMeters": 0.0 },
                      "velocity": { "speedMsec": 0.0, "headingDegrees": 0.0, "verticalSpeedMsec": 0.0 } },
      "aircraftB": { "...": "same shape as aircraftA" },
      "separationMeters": 0.0
    }
  ]
}
```

- `validationResult` is `"PASS"` when `total_violations_recorded == 0`, `"FAIL"` otherwise (AC04).
- `violationEvents` is empty when no violation occurred; each entry carries `timestamp` (simulation time), `wallClockTime`, and the `position`/`velocity` of both aircraft involved (AC03).

---

## User Interaction Flow

1. The Flight Control Operator runs a simulation from the JavaFX `SimulationUI`.
2. At the end of the run, `simulation_engine` enters `THREAD_REPORT`, forks and execs `report_generator`, which writes `simulation_report_<timestamp>.json` into `aisafe-simulation/` (AC01).
3. `SimulationUI.showReportDialog()` calls `SimulationReportParser.findAndParse()`, which locates the most recently modified `simulation_report_*.json` and parses it.
4. The dialog shows: validation result (PASS/FAIL, AC04), total flights and total violations (AC02), and — if any — a table of violation events with simulation time, the two callsigns and their separation (AC03).

---

## Key Classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `report_generator.c` | `aisafe-simulation/src/main/c` | Reads `SimulationConfig`/`SimulationReport` from shared memory `/aisafe_sim`, writes the timestamped JSON report (AC01–AC04) |
| `simulation_engine.c` | `aisafe-simulation/src/main/c` | `THREAD_REPORT` phase — forks/execs `report_generator` once the simulation finishes |
| `SimulationReportParser` | `javafxui.presentation.simulation` | Finds the newest `simulation_report_*.json`, parses totals/validation result/violation events |
| `SimulationUI` | `javafxui.presentation.simulation` | `showReportDialog()` — presents the parsed report to the operator |
| `SimulationReport` | `simulationReport.domain` | JPA Aggregate Root mirroring the summary (`totalFlights`, `validationResult`, `generatedAt`, `simulationId`) |
| `SimulationReportRepository` | `simulationReport.repository` | Repository abstraction for `SimulationReport` |
| `JpaSimulationReportRepository` / `InMemorySimulationReportRepository` | `aisafe.persistence.impl` | Persistence implementations |

---

## Persistence Status

The `simulationReport.domain.SimulationReport` aggregate and its repositories exist and are covered by `SimulationReportTest`, but **no controller currently calls `SimulationReportRepository.save(...)`** — the Java-side persistence of report summaries is not yet wired into the simulation flow. AC01–AC04 are fully satisfied today by the **file-based** report (`simulation_report_*.json`) produced by `report_generator` and read by `SimulationReportParser`; persisting a `SimulationReport` aggregate per run (e.g., for historical querying) is a natural follow-up but not required by the current acceptance criteria.

---

## Testing Notes

- `SimulationReportTest` (`aisafe.core/src/test/java/simulationReport/domain`) covers construction, validation and equality of the `SimulationReport` aggregate.
- There is no automated test for `report_generator.c` or `SimulationReportParser` — these are currently exercised manually via `SimulationUI`.

---

## Relação com outras User Stories

- **US110** — Environmental influences on simulation: defines the lockstep pipeline (`THREAD_REPORT` is one of its phases).
- **US112** — Monthly report generation: envisioned as a separate, foundational reporting framework; currently not implemented and does not reuse `SimulationReport`.
