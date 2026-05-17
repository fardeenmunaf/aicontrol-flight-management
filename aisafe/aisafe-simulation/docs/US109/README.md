# US109 — Simulation Report Generation

## User Story

> As a **Flight Control Operator**, I want a comprehensive report that details the simulation outcomes - including flight execution statuses, safety violation events (with timestamps, positions and velocity vectors), and overall validation results - so that I can assess the safety and performance of the flights post-simulation.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC01 | Only an authenticated **FCO** can request a simulation report |
| AC02 | The report generation thread aggregates all data once the simulation concludes |
| AC03 | The report includes the total number of flights and each flight's individual execution status |
| AC04 | Safety violation events are listed with timestamp, position, and velocity vector |
| AC05 | The final validation result (pass/fail) is clearly indicated in the report |
| AC06 | The complete report is saved to a file for future reference |
| AC07 | The report thread is notified via condition variables when a safety violation occurs during simulation |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe.core` | Application controller (`SimulationReportController`) and domain model (`SimulationReport`, `FlightStatus`, `ViolationEvent`) |
| `aisafe-simulation/src/main/c` | C-based report generator (`report_generator.c`, `structs.h`) |

---

## Class Diagram

![Class Diagram](class-diagram.svg)

---

## Sequence Diagram — Report Generation & Viewing

![Sequence Diagram](sequence-simulation-report.svg)

---

## Report Structure

### JSON Report Schema

```json
{
  "reportVersion": "1.0",
  "reportType": "SimulationReport",
  "reportGeneratedAt": "2026-05-17T15:30:45",
  
  "simulation": {
    "totalFlights": 20,
    "startTime": 0.0,
    "endTime": 3600.0,
    "durationSeconds": 3600.0,
    "wallClockStart": "2026-05-17T10:00:00"
  },
  
  "safetyValidation": {
    "totalViolationsDetected": 11,
    "validationResult": "FAIL",
    "maxViolationsThreshold": 0
  },
  
  "flightExecutionStatuses": [
    {
      "flightId": 0,
      "callsign": "TAP001",
      "completed": true,
      "hadViolation": false,
      "fuelRemainingKg": 1200.5,
      "legsCompleted": 3
    },
    ...
  ],
  
  "violationEvents": [
    {
      "violationId": 0,
      "timestamp": 120.5,
      "wallClockTime": "2026-05-17T10:02:00",
      "aircraftA": {
        "flightId": 1,
        "callsign": "TAP002",
        "position": {
          "latitude": 38.7223,
          "longitude": -9.1393,
          "altitudeMeters": 1500.0
        },
        "velocity": {
          "speedMsec": 150.0,
          "headingDegrees": 45.0,
          "verticalSpeedMsec": 5.0
        }
      },
      "aircraftB": {
        "flightId": 5,
        "callsign": "TAP006",
        "position": {
          "latitude": 38.7225,
          "longitude": -9.1395,
          "altitudeMeters": 1495.0
        },
        "velocity": {
          "speedMsec": 140.0,
          "headingDegrees": 225.0,
          "verticalSpeedMsec": -3.0
        }
      },
      "separationMeters": 850.0
    },
    ...
  ]
}
```

---

## Key Components

### Report Generator Process (`report_generator.c`)

**Purpose**: Separate process that reads simulation data from shared memory and generates JSON report after simulation completes.

**Process Flow**:
1. Opens shared memory created by `simulation_engine` (waits up to 120 seconds)
2. Polls `report_ready` flag until simulation completes
3. Reads all flight statuses and violation events from shared memory
4. Generates formatted JSON report with human-readable timestamps
5. Writes report to file: `simulation_report_YYYYMMDD_HHMMSS.json`
6. Cleans up shared memory via `shm_unlink()`

**Key Features**:
- Robust shared memory open with retry loop (120-second timeout)
- Polls `report_ready` flag with 1-hour timeout
- Handles JSON string escaping for callsigns and special characters
- Converts simulation timestamps to ISO 8601 format using `wallClockStart` reference

### Shared Memory Data Structures (`structs.h`)

**ViolationEvent**:
- Records timestamp, aircraft IDs, positions, velocities, separation distance
- Full kinematic context for safety analysis

**FlightStatus**:
- Per-flight completion status, violation flag, fuel remaining, legs completed
- Summary information for each flight in the simulation

**SimulationReport**:
- Nested within `SharedMemoryData`
- Holds simulation overview, violation events array, flight statuses array
- Includes `report_ready` flag for synchronization

### Simulation Engine Integration (`simulation_engine.c`)

**Report Finalization**:
- `finalize_simulation_report()` populates flight statuses and sets `report_ready` flag
- Called when simulation completes (all flights finished)
- Does NOT unlink shared memory (responsibility moved to report generator)

**Violation Recording**:
- `check_violations()` records each detected violation to `violations[]` array
- Captures full kinematic state: positions, velocities, separation distance
- Records timestamp (simulation time)

---

## User Interaction Flow

1. Flight Control Operator logs in and navigates to **View Simulation Report**.
2. System displays list of available reports (sorted by date, most recent first).
3. FCO selects a report to view.
4. `SimulationReportController` verifies the caller has the `FLIGHT_CONTROL_OPERATOR` role.
5. The controller loads the report JSON file:
   - Parses simulation metadata (flights count, violations, result)
   - Displays flight execution summary
   - Displays violation details with timestamps and positions
6. FCO can optionally export or print the report.

---

## Key Classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `SimulationReportController` | `simulation.application` | Enforces FCO authorization, loads and parses report files |
| `SimulationReportUI` | `backoffice.presentation.simulation` | Console form — displays report summary, allows report viewing/export |
| `SimulationReport` | `simulation.domain` | Aggregate root — holds simulation metadata, flight statuses, violation events |
| `FlightStatus` | `simulation.domain` | Value object — per-flight execution summary |
| `ViolationEvent` | `simulation.domain` | Value object — single safety violation with full kinematic context |
| `SimulationReportRepository` | `simulation.repository` | Repository for storing/retrieving report files and metadata |

---

## Report Generation Process

### Synchronization Points

1. **Simulation Completes**:
   - All flights finished or timeout reached
   - `finalize_simulation_report()` populates flight statuses
   - Sets `report_ready = true` in shared memory

2. **Report Generator Detects Readiness**:
   - Polls `shm->report.report_ready` flag
   - When true, begins JSON generation

3. **File Written**:
   - JSON report written to disk
   - Filename format: `simulation_report_YYYYMMDD_HHMMSS.json`
   - Shared memory unlinked (cleanup)

---

## Timestamp Formatting

### Simulation Time vs. Wall-Clock Time

- **Simulation Time**: 0.0 to N seconds (relative to simulation start)
- **Wall-Clock Time**: Human-readable ISO 8601 string (e.g., `2026-05-17T10:02:00`)

**Conversion Formula**:
```c
wall_time = (time_t)(wallClockStart + simulationSeconds);
strftime(buf, len, "%Y-%m-%dT%H:%M:%S", localtime(&wall_time));
```

**Example**:
- `wallClockStart`: `1715945400` (UNIX timestamp for 2026-05-17 10:00:00)
- Violation timestamp in simulation: `120.5` seconds
- Wall-clock time: `2026-05-17T10:02:00` (10:00:00 + 2 minutes)

---

## Test Cases

### TC01 – Report generated after successful simulation
**Given** an authenticated FCO ran a simulation that completed without violations  
**When** the simulation ends  
**Then** the system generates a report with all flight statuses and a pass result, saved to a file

### TC02 – Report includes safety violation details
**Given** a simulation where two flights triggered a safety violation  
**When** the simulation ends  
**Then** the report lists the violation with its timestamp, positions, and velocity vectors of both aircraft

### TC03 – Pass/fail result correctly reflected
**Given** a simulation that ended with unresolved safety violations  
**When** the report is generated  
**Then** the final validation result is marked as fail

### TC04 – Report file persists after simulation
**Given** a completed simulation  
**When** the FCO accesses the report at a later time  
**Then** the report file is retrievable and its contents match the simulation outcome

### TC05 – Non-FCO cannot access report
**Given** an authenticated user with role Pilot  
**When** they attempt to retrieve a simulation report  
**Then** the system denies access with a 403/Forbidden error

### TC06 – Report thread notified on violation during simulation
**Given** the safety violation detection thread detects a conflict mid-simulation  
**When** the violation is confirmed  
**Then** the report thread is notified via condition variable and logs the event in real time

---

## Done Concept

- A Flight Control Operator (FCO) can access a final simulation report after a simulation run concludes
- The report lists the total number of flights and their individual execution statuses
- All safety violation events are recorded with timestamps, positions, and velocity vectors
- The overall simulation result (pass/fail) is clearly stated
- The report is saved to a file
- Only authenticated FCOs can perform this action
- Test coverage of controller and domain packages ≥ 90%
- Code compiles and all tests pass
- Technical documentation updated in the repository (analysis, design, tests) in Markdown format

---

## Notes

- Report generation uses separate POSIX process (not pthread) to avoid blocking the simulation
- Shared memory persists until `report_generator` completes cleanup
- JSON report is human-readable and machine-parseable for further analysis
- Timestamps are formatted in ISO 8601 for international standardization and easy parsing
- Violation events capture full kinematic state for forensic analysis of near-miss incidents

