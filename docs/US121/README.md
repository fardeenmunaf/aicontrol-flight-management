# US121 — Create a Flight Plan from a File

## User Story

> As a **Pilot**, I want to create a valid flight plan from a file.
> There may be multiple file formats, and I want to validate the file format before using it.

---

## Acceptance Criteria

| # | Criteria |
|---|-----------|
| AC01 | The flight plan file must conform to the Flight DSL grammar. |
| AC02 | The file must be validated according to US120 (syntactic + semantic checks). |
| AC03 | Invalid files must produce meaningful, line-accurate error messages. |
| AC04 | Only valid flight plans may be imported and persisted. |
| AC05 | On success, each flight plan in the file is saved to the database and a JSON export is written for the simulator. |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe.app.airtransport.console` | Console UI (`CreateFlightPlanUI`) — prompts for a file path, delegates to the service, prints per-plan results |
| `aisafe.app.javafx` | JavaFX UI (`FlightPlanUI`) — "Import from File" button opens a `FileChooser`; result shown in an `Alert` dialog |
| `aisafe.flight.dsl` | `FlightPlanCreationService` — single entry point for file import; `FlightDslProcessor` — ANTLR4 parse pipeline; `FlightSemanticValidator` — US120 semantic checks; `FlightPlanAstMapper` — AST→aggregate translation; `ExportSimulationFlightPlanService` — JSON export |
| `aisafe.core` | `CreateFlightPlanController` — authorization check + `FlightPlanRepository.save()`; domain aggregates `FlightPlan`, `FlightLeg`, `Segment`, `ProfileEntry`, `AltitudeSlot` |
| `aisafe.persistence.impl` | `JpaFlightPlanRepository`, `JpaAirportRepository`, `JpaNodeRepository` |


---

## Flow Description

### Entry Points

**Console** (`CreateFlightPlanUI`): the user selects option `1. Import from a DSL file`, types a file path, and the UI calls `FlightPlanCreationService.createFromFile(Paths.get(path))`.

**JavaFX** (`FlightPlanUI`): the user clicks "Import from File"; a `FileChooser` filtered for `.dsl`, `.flight`, and `.txt` files opens. On selection, the UI calls `creationService.createFromFile(file.toPath())`.

Both entry points share the same service and validation pipeline — no logic is duplicated.

### DSL Processing Pipeline

```
FlightDslProcessor.process(file / text)
  1. CharStream from path or string
  2. FlightDSLLexer  ──── ANTLR4 tokenisation
  3. FlightDSLParser ──── syntactic parse → ParseTree
      └── ThrowingErrorListener: any syntax error → ParseCancellationException
  4. FlightAstBuilderVisitor.visit(tree) → FlightDslDocument (typed AST)
  5. FlightSemanticValidator.validate(document) → List<SemanticError>
  6. return FlightDslProcessingResult (document + errors)
```

A `ParseCancellationException` from step 3 is caught by `FlightPlanCreationService` and surfaced as a `CreationResult` error, so the UI always receives structured feedback — never a raw stack trace.

### Semantic Validation (US120)

`FlightSemanticValidator` enforces the following rules, each mapped to a `SemanticError` with a line and column number:

| Rule | Check |
|------|-------|
| Flight designator format | Regex `[A-Z]{2,3}[0-9]{1,4}[A-Z]?` |
| Duplicate designator | No two plans in the same file may share an identifier |
| Pilot reference | Must be a valid email address |
| Departure not in the past | `departureDateTime >= now()` |
| Arrival after departure | `arrivalDateTime > departureDateTime` (per leg) |
| Leg continuity | Arrival airport of leg N must equal departure airport of leg N+1 |
| Route coherence | `routeOrigin == firstLeg.departure`, `routeDestination == lastLeg.arrival` |
| Segment continuity | `segment[i].to == segment[i+1].from` (coordinates must chain exactly) |
| Segment altitude direction | climb → altitude increasing; descend → altitude decreasing; cruise → altitude constant |
| Altitude bounds | −500 m / −1 640 ft (Dead-Sea floor) to 13 700 m / 45 000 ft (commercial ceiling) |
| Wind speed | 0 < speed ≤ 25 m/s |
| Wind direction | 0–359° |
| Fuel quantity | Strictly positive |
| Profile/segment consistency | Climb profile entries required if climb segment present; same for cruise and descend |
| Profile altitude alignment | Climb profile min altitude must match climb segment departure altitude; max must match cruise altitude |

### AST → Aggregate Mapping

`FlightPlanAstMapper.map(FlightPlanAst)` resolves external references and builds the JPA-ready aggregate:

```
For each LegAst:
  ├── departure/arrival airport codes → AirportRepository → AirportId
  ├── segment coordinates → NodeRepository.save(new Node(...)) → NodeId
  │     (coordinates are de-duplicated: same lat/lon/alt reuses the same Node)
  ├── fuel quantity → FlightLeg field
  ├── profile entries → FlightLeg.addProfileEntry(...)
  └── segments + altitude slots → FlightLeg.addSegment(...)
return new FlightPlan(id, designator, type, aircraftId, pilotRef, route, legs)
```

### Persistence and Export

For each successfully mapped `FlightPlan`:

1. `CreateFlightPlanController.create(plan)` — verifies the caller holds the `PILOT` or `AIR_TRANSPORT_COMPANY_COLLABORATOR` role, then calls `FlightPlanRepository.save(plan)`.
2. `ExportSimulationFlightPlanService.export(ast)` — writes a JSON file to `aisafe-simulation/flightplansSim/` for consumption by the flight simulator. Export failure is captured as an error in `CreationResult` but does **not** roll back the DB save.

### Result Reporting

`CreationResult` accumulates both successfully created plans and error messages:

```
result.hasErrors() → print each error (line-accurate from SemanticError)
result.created()   → print each plan: designator + FlightPlanId
```

If the file contains multiple plans, partial success is possible: valid plans are persisted even if others in the same file fail.

---

## Key Classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `CreateFlightPlanUI` | `airtransport.presentation.flightplan` | Console UI: path prompt, mode selection, result display |
| `FlightPlanUI` | `javafxui.presentation.flightplan` | JavaFX UI: FileChooser import, table refresh, result Alert |
| `FlightPlanCreationService` | `flight.dsl.mapping` | Orchestrates parse → validate → map → persist → export; shared by both UIs |
| `FlightDslProcessor` | `flight.dsl.processing` | ANTLR4 parse pipeline: lex → parse → build AST → semantic validate |
| `FlightDSLLexer` / `FlightDSLParser` | `flight.dsl` (ANTLR4 generated) | Tokenise and parse DSL text; throw on first syntax error |
| `FlightAstBuilderVisitor` | `flight.dsl.visitor` | Visitor over the ANTLR parse tree; produces a typed `FlightDslDocument` |
| `FlightSemanticValidator` | `flight.dsl.validation` | US120 semantic rules; produces `List<SemanticError>` with line/column |
| `FlightPlanAstMapper` | `flight.dsl.mapping` | Translates `FlightPlanAst` → `FlightPlan` aggregate; resolves airports and nodes |
| `ExportSimulationFlightPlanService` | `flight.dsl.mapping` | Writes per-plan JSON to the simulator output directory |
| `CreateFlightPlanController` | `flightPlan.application` | Role-based authorization + `FlightPlanRepository.save()` |
| `FlightPlan` | `flightPlan.domain` | Aggregate Root — holds designator, type, aircraft, pilot, route, legs |
| `FlightLeg` | `flightPlan.domain` | Entity — departure/arrival, fuel, profile entries, segments |
| `Segment` | `flightPlan.domain` | Value Object — from/to `NodeId`, wind, altitude slots, mode |
| `ProfileEntry` | `flightPlan.domain` | Value Object — climb/cruise/descend speed and altitude data |
| `AltitudeSlot` | `flightPlan.domain` | Value Object — min/max/width altitude corridor |
| `CreationResult` | `flight.dsl.mapping` | DTO — accumulates created plans and error strings for the UI |
| `FlightDslDocument` | `flight.dsl.model` | Root AST node: list of `FlightPlanAst` with source-location registry |
| `FlightPlanRepository` | `flightPlan.repository` | Repository interface for `FlightPlan` aggregate |
| `AirportRepository` | `airport.repositories` | Resolves IATA/ICAO codes to `AirportId` during mapping |
| `NodeRepository` | `node.repository` | Saves or reuses coordinate-based `Node` entities during mapping |

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

- **Ubiquitous Language**: "Flight Plan," "Flight Designator," "Leg," "Segment," "Profile," "Altitude Slot" are terms from the Aviation Operations subdomain and map directly to DSL keywords and domain classes.
- **Bounded Context**: The `aisafe.flight.dsl` module is a self-contained translation context. It receives a plain file path and produces a `FlightPlan` aggregate — all internal parsing, AST building, and semantic validation are invisible to the caller.
- **Aggregate Design**: `FlightPlan` is the aggregate root. `FlightLeg`, `Segment`, `ProfileEntry`, and `AltitudeSlot` are owned entities/value objects; they are only reachable through the root and never persisted independently.
- **Factory Method (Domain)**: `FlightPlanAstMapper.map()` acts as a factory that builds a valid `FlightPlan` aggregate from an already-validated AST. Only a valid AST reaches the mapper — invalid input never produces a partially-built aggregate.
- **Repository Pattern**: `FlightPlanRepository`, `AirportRepository`, and `NodeRepository` each serve exactly one aggregate. `AirportRepository` is read-only during flight-plan creation; `NodeRepository` creates shared junction nodes; `FlightPlanRepository` is the single write target.

### Gang of Four Design Patterns

| Pattern | Application | Benefit |
|---------|------------|---------|
| **Visitor** | `FlightAstBuilderVisitor` traverses the ANTLR `ParseTree` to produce a typed `FlightDslDocument` | Parsing logic is decoupled from the grammar rules; the grammar can change without touching the AST builder |
| **Service Layer** | `FlightPlanCreationService` encapsulates the full pipeline behind a single method | Both UI layers call one method; neither is aware of ANTLR, the AST, or the mapper |
| **Template Method** | `AbstractUI.show()` in eapli calls the overridable `doShow()` | `CreateFlightPlanUI` implements only domain-specific logic; lifecycle management is inherited |
| **Result Object** | `CreationResult` collects all outcomes (created plans + errors) | The caller receives a complete picture; partial success is handled uniformly without exceptions |

### SOLID Principles

- **Single Responsibility Principle (SRP)**: Each class in the pipeline has one job — `FlightDslProcessor` parses, `FlightSemanticValidator` validates, `FlightPlanAstMapper` translates, `CreateFlightPlanController` authorises and persists, `ExportSimulationFlightPlanService` exports.
- **Open/Closed Principle (OCP)**: New semantic rules can be added to `FlightSemanticValidator` without changing the processor, mapper, or controller.
- **Dependency Inversion Principle (DIP)**: `CreateFlightPlanController` depends on the `FlightPlanRepository` interface. `FlightPlanCreationService` depends on repository interfaces injected at construction — no coupling to JPA implementations.
- **Interface Segregation Principle (ISP)**: Each repository interface exposes only the queries its aggregate requires; the creation pipeline never calls unrelated methods.

### Two-Phase Validation

Validation is split into two phases with distinct error characteristics:

1. **Syntactic (ANTLR4)**: `ThrowingErrorListener` stops at the first lexer or parser error and reports the exact line and character position. This is a hard stop — a syntactically broken file cannot produce an AST, so no semantic work is attempted.
2. **Semantic (`FlightSemanticValidator`)**: All rules are evaluated in a single pass over the full AST. Every violation is accumulated into the error list — the user sees all problems in one response, not one error per run. Each `SemanticError` carries a line and column derived from the source-location registry populated during AST construction.

This two-phase strategy follows the *fail-fast* principle at the syntax level (one error is enough to reject unparseable input) while maximising feedback at the semantic level (all rule violations are reported together).
