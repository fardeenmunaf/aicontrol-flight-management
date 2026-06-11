# US085 — Test/Validate a Flight Plan

## User Story

> As a **Pilot**, I want to test/validate a flight plan, so that I can confirm it is correct before it is used operationally.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC1 | The stored DSL text of the flight plan must be fully re-validated through **lexical, syntactic, and semantic analysis**. |
| AC2 | A **C physical validator** must check fuel sufficiency (last leg's fuel ≥ destination airport's minimum required arrival fuel) and that all segment altitudes are within their declared altitude slots. |
| AC3 | The physical validation is performed by a native C program (`flight_plan_validator`) that receives a JSON description of the flight plan and returns a JSON result with a pass/fail flag and a list of failures. |
| AC4 | If **all checks pass**, the flight plan's status must be updated from `DRAFT` to `TESTED`. |
| AC5 | Only authenticated users with the **PILOT** role may trigger validation. |
| AC6 | A pilot may only validate their **own** flight plans (ownership check). |
| AC7 | Only flight plans in **DRAFT** status can be submitted for validation. |
| AC8 | Both the **console UI** and the **JavaFX UI** must expose the validate feature. |

---

## Implementation Overview

This US spans four modules and one native component:

| Module | Role |
|--------|------|
| `aisafe.core` | `FlightPlan` aggregate (stores DSL text, `markAsTested`), `ValidateFlightPlanController`, `FlightPlanValidationResult` |
| `aisafe.flight.dsl` | `ValidateFlightPlanService` — orchestrates the full pipeline (DSL re-parse → physical check → status update) |
| `aisafe-simulation` (C) | `flight_plan_validator` — native binary that reads a JSON input file and writes a JSON result to stdout |
| `aisafe.app.airtransport.console` | `ValidateFlightPlanUI` / `ValidateFlightPlanAction` — interactive console flow |
| `aisafe.app.javafx` | Validate button in `FlightPlanUI` — JavaFX table action for DRAFT plans |

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

- **Ubiquitous Language**: Terms like `validate`, `test`, `DRAFT`, `TESTED`, `fuel sufficiency`, and `altitude slot` map directly to the domain and are used consistently across all layers.
- **Bounded Context**: Validation belongs to the Flight Planning bounded context. It operates entirely on the `FlightPlan` aggregate and uses the Airport/AirControlArea aggregates only to read constraint data.
- **Aggregate Root**: `FlightPlan` is the aggregate root. Only the root exposes `markAsTested()`, ensuring the status transition is controlled and guarded by a domain-level invariant.
- **Value Objects**: `FlightPlanId`, `FlightPlanStatus`, airport codes, and fuel quantities are modeled as value-like concepts with explicit validation rules.
- **Repository Pattern**: `ValidateFlightPlanController` uses `FlightPlanRepository` to load the plan by ID and persist the status change, decoupling domain logic from storage.

### Gang of Four (GoF) Patterns

| Pattern | Application | Benefit |
|---------|-------------|---------|
| **Strategy** | `ValidateFlightPlanService` is swappable — any alternative validation logic can implement the same interface | Validation rules can evolve without touching the controller or UI |
| **Facade** | `ValidateFlightPlanService` hides the complexity of DSL re-parsing, JSON serialisation, process spawning, and result parsing behind a single `validate(planId)` call | The UI and controller stay simple |
| **Data Transfer Object (DTO)** | `FlightPlanValidationResult` carries DSL errors and physical failures without exposing parser or process internals | The presentation layer never interacts with ANTLR or ProcessBuilder |
| **Template Method** | `FlightDslProcessor.process()` defines the fixed pipeline (lex → parse → AST → semantic) that `ValidateFlightPlanService` reuses | Validation re-uses the same pipeline as creation |

### SOLID Principles

- **Single Responsibility Principle (SRP)**: `ValidateFlightPlanController` handles auth/ownership/status guards; `ValidateFlightPlanService` owns the validation pipeline; `flight_plan_validator.c` performs only the physical checks.
- **Open/Closed Principle (OCP)**: New physical checks can be added inside the C binary without changing any Java code; new DSL semantic rules can be added to the validator without touching the service.
- **Liskov Substitution Principle (LSP)**: `FlightPlanRepository`, `AirportRepository`, and `ValidateFlightPlanController` can all be replaced by in-memory stubs in tests without breaking the service behaviour.
- **Interface Segregation Principle (ISP)**: `FlightPlanValidationResult` exposes only `dslErrors()`, `physicalFailures()`, and `passed()` — callers never see ANTLR or process internals.
- **Dependency Inversion Principle (DIP)**: `ValidateFlightPlanService` depends on `ValidateFlightPlanController` (which itself uses the `FlightPlanRepository` interface) and `AirportRepository`, never on concrete JPA implementations.

### Data Transfer Objects (DTOs)

- **`FlightPlanValidationResult`**: Carries the outcome of the full validation pipeline from `ValidateFlightPlanService` to the presentation layer.

  | Field | Meaning |
  |-------|---------|
  | `dslErrors()` | Lexical, syntactic, or semantic errors from DSL re-parsing |
  | `physicalFailures()` | Failures reported by the C validator (fuel, altitude) |
  | `physicalCheckRan()` | Whether the C binary was executed (false if DSL failed first) |
  | `passed()` | `true` only when both DSL and physical checks produced no issues |

---

## Validation Pipeline

```
FlightPlanId
      │
      ▼
┌──────────────────────────────────────┐
│ ValidateFlightPlanController          │
│  • ensureAuthenticatedUserHasAnyOf(PILOT)  │
│  • ownership check (pilotRef == email)     │
│  • status check (must be DRAFT)       │
└──────────────────────────────────────┘
      │
      ▼
┌──────────────────────────────────────┐
│ AC1 — DSL Re-validation               │
│  FlightDslProcessor.process(dslText) │
│  Lexical → Syntactic → AST → Semantic│
└──────────────────────────────────────┘
      │ (only if DSL is valid)
      ▼
┌──────────────────────────────────────┐
│ AC2/AC3 — Physical Validation (C)    │
│  Build JSON input from AST           │
│  Spawn flight_plan_validator <file>  │
│  Parse JSON output (passed + failures)│
└──────────────────────────────────────┘
      │ (only if all checks passed)
      ▼
┌──────────────────────────────────────┐
│ AC4 — Status Transition               │
│  plan.markAsTested()                 │
│  flightPlanRepository.save(plan)     │
└──────────────────────────────────────┘
      │
      ▼
  FlightPlanValidationResult
  (dslErrors, physicalFailures, passed)
```

---

## C Physical Validator

The native component lives in `aisafe-simulation/src/main/c/flight_plan_validator.c` and is built automatically if not found when validation is first triggered.

### Input (JSON file path passed as `argv[1]`)

```json
{
  "flightDesignator": "TP1234",
  "minimumRequiredArrivalFuel": 5000.0,
  "legs": [
    {
      "legNumber": 1,
      "fuelQuantity": 40200.0,
      "segments": [
        {
          "segmentNumber": 1,
          "mode": "climb",
          "fromAltitude": 69.0,
          "toAltitude": 9249.0,
          "altitudeUnit": "m",
          "minimumAltitude": 0.0,
          "maximumAltitude": 9249.0
        }
      ]
    }
  ]
}
```

### Output (written to stdout)

```json
{
  "passed": true,
  "failures": []
}
```

### Physical Checks Performed

| Check | Rule | Failure Message |
|-------|------|-----------------|
| **Fuel sufficiency** | Last leg's `fuelQuantity` ≥ `minimumRequiredArrivalFuel` | `Insufficient arrival fuel: X kg remaining, minimum required is Y kg` |
| **Altitude bounds** | Every segment's `fromAltitude` and `toAltitude` ≥ `minimumAltitude` | `Leg N, segment M: altitude X m is below the minimum allowed Y m` |

### Automatic Build

If the binary is not found at startup, `ValidateFlightPlanService` automatically runs `make flight_plan_validator` in the `aisafe-simulation` directory. No manual compilation is needed.

---

## DSL Text Storage

To enable AC1, the full DSL text is stored in the `FlightPlan` entity when the plan is first created:

```java
// FlightPlan.java
@Lob
@Column(name = "dsl_text", columnDefinition = "TEXT")
private String dslText;
```

This means only flight plans created **after** this feature was introduced will have stored DSL text and can be fully re-validated. Legacy plans without DSL text receive a descriptive error message instead.

The corresponding DB column:

```sql
ALTER TABLE FlightPlan ADD COLUMN IF NOT EXISTS dsl_text TEXT;
```

---

## Status Transition

```
DRAFT ──[all checks pass]──▶ TESTED
DRAFT ──[any check fails]──▶ DRAFT  (unchanged)
```

The transition is enforced at the domain level:

```java
// FlightPlan.java
public void markAsTested() {
    if (this.status != FlightPlanStatus.DRAFT) {
        throw new IllegalStateException("Only a DRAFT FlightPlan can be marked as tested");
    }
    this.status = FlightPlanStatus.TESTED;
}
```

---

## Key Classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `FlightPlan` | `aisafe.core` / `flightPlan.domain` | Aggregate root; stores DSL text; exposes `markAsTested()` |
| `ValidateFlightPlanController` | `aisafe.core` / `flightPlan.application` | Auth, ownership, and status guards; persists status change |
| `FlightPlanValidationResult` | `aisafe.core` / `flightPlan.application` | DTO carrying DSL errors, physical failures, and overall pass/fail |
| `ValidateFlightPlanService` | `aisafe.flight.dsl` / `flight.dsl.mapping` | Full validation pipeline — re-parse DSL, invoke C binary, update status |
| `flight_plan_validator.c` | `aisafe-simulation` / `src/main/c` | Native C binary; checks fuel sufficiency and altitude bounds |
| `ValidateFlightPlanUI` | `aisafe.app.airtransport.console` | Interactive console UI; lists DRAFT plans, runs validation, shows result |
| `ValidateFlightPlanAction` | `aisafe.app.airtransport.console` | Console menu wiring — delegates to `ValidateFlightPlanUI.show()` |
| `FlightPlanUI` | `aisafe.app.javafx` | JavaFX flight plan screen; Validate button on each DRAFT DB plan row |

---

## User Interaction Flow

### Console UI

1. The pilot selects **"Test/Validate Flight Plan"** from the main menu.
2. All DRAFT flight plans belonging to the logged-in pilot are listed.
3. The pilot selects one plan by number.
4. The full validation pipeline runs (DSL re-parse → physical check).
5. The result is printed:
   - **Pass**: `✓ Flight plan '<designator>' validated successfully. Status: TESTED`
   - **Fail**: lists all DSL errors and/or physical failures with descriptions

### JavaFX UI

1. The pilot opens the **Flight Plans** screen.
2. Each row for a DB DRAFT plan shows a **Validate** button in the Action column.
3. Clicking **Validate** runs the pipeline and shows an information/warning dialog with the result.
4. On success the table refreshes automatically, showing the plan as TESTED.

---

## Test Coverage

| Test class | What is verified |
|------------|-----------------|
| `ValidateFlightPlanControllerTest` | Auth (PILOT role required), ownership rejection, non-DRAFT rejection, successful `markAsTested`, repository persistence |
| `FlightPlanCreationServiceTest` | Creating a valid plan stores it in DRAFT; export failures are reported as warnings (not errors), so `hasErrors()` remains false |
