# US081 — Create a Flight Plan from a File

## User Story

> As a **pilot**, I want to create a flight plan from a file.
> There may be multiple file formats, and I want to validate the file format before using it.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC1 | The flight plan file must conform to the **Core Flight DSL**. |
| AC2 | The file must be validated through **lexical, syntactic, and semantic analysis**. |
| AC3 | Invalid files must produce **meaningful error messages** indicating type, line, and column. |
| AC4 | Only **valid flight plans** may be imported and used by the system. |

---

## Implementation Overview

This US is implemented across two modules:

| Module | Role |
|--------|------|
| `aisafe.flight.dsl` | Defines the grammar, AST, processing pipeline, and semantic validator |
| `aisafe.app.airtransport.console` | Provides the interactive pilot UI (`CreateFlightPlanUI`) that builds the DSL text, validates it, and imports the result |

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

- **Ubiquitous Language**: Terms such as `flight`, `leg`, `route`, `segment`, `departure`, and `arrival` are used consistently throughout the DSL, parser, and UI.
- **Bounded Context**: Flight plan creation is a dedicated bounded context focused on a pilot’s planning workflow, separate from other concerns like weather or fleet management.
- **Aggregate Root**: The parsed flight plan document acts as the aggregate root for the imported plan and its legs/segments.
- **Value Objects**: Flight designator, airport codes, coordinates, dates, times, and unit-bearing quantities are modeled as value-like concepts with explicit validation rules.
- **Repository Pattern**: The import step uses application services and repositories to persist only valid flight plans, decoupling parsing from storage.

### Gang of Four (GoF) Patterns

| Pattern | Application | Benefit |
|---------|------------|---------|
| **Visitor** | `FlightAstBuilderVisitor` | Traverses the parse tree and builds the typed AST |
| **Listener** | `FlightStructureListener` | Collects structural metrics while the tree is processed |
| **Strategy** | `FlightSemanticValidator` | Encapsulates semantic rules so the validation logic can evolve independently |
| **Factory** | `CreateFlightPlanUI` / DSL builder step | Centralizes creation of the DSL text and its import-ready representation |
| **Data Transfer Object (DTO)** | `FlightDslProcessingResult` | Carries parsed document, summary, and errors without exposing parser internals |

### SOLID Principles

- **Single Responsibility Principle (SRP)**: `FlightAstBuilderVisitor` builds the AST; `FlightSemanticValidator` checks business rules; `CreateFlightPlanUI` handles user interaction.
- **Open/Closed Principle (OCP)**: New semantic rules can be added to the validator without rewriting the parser pipeline.
- **Liskov Substitution Principle (LSP)**: Alternative validators or repositories can replace current implementations without breaking the workflow.
- **Interface Segregation Principle (ISP)**: Each component exposes only the methods it needs (`validate`, `visit`, `process`, etc.), avoiding bloated interfaces.
- **Dependency Inversion Principle (DIP)**: The UI and application services depend on abstractions such as validators and repositories rather than concrete parsing/storage details.

### Data Transfer Objects (DTOs)

- **`FlightDslProcessingResult`**: Transfers the parsed document, structural summary, and error list from the DSL module to the UI/application layer.
- **Benefit**: The presentation layer never needs to interact directly with ANTLR internals, which keeps the code easier to test and evolve.

### Factory Pattern

The DSL builder acts as a controlled factory for flight plan text:

```java
FlightDslProcessingResult result = flightDslProcessor.process(dslText);
if (result.isValid()) {
    importFlightPlanController.importPlan(result.document());
}
```

Factory responsibilities:
- Construct a valid processing result from raw DSL text
- Encapsulate parser/validator initialization
- Keep the UI focused on gathering user input rather than building domain objects directly

---

## Core Flight DSL

The grammar is defined with **ANTLR 4** in:

```
aisafe/aisafe.flight.dsl/src/main/antlr4/flight/dsl/FlightDSL.g4
```

### Grammar Structure

A valid flight plan document can contain **one or more `flight` blocks**. Each block follows this structure:

```
flight <DESIGNATOR> {
    type:     regular | charter;
    aircraft: <AIRCRAFT_ID>;
    route:    <ROUTE_REF> from <ORIGIN> to <DESTINATION>;

    leg {
        departure {
            airport: <CODE>;
            date:    YYYY-MM-DD;
            time:    HH:MM;
        }
        arrival {
            airport: <CODE>;
            date:    YYYY-MM-DD;
            time:    HH:MM;
        }
        fuel {
            quantity: <NUMBER> kg | l;
        }
        profile {                          // optional
            climb   [ altitude: <N> m|ft, speed: <N> kts|m/s ];
            descend [ altitude: <N> m|ft, speed: <N> kts|m/s, rate: <NEGATIVE_NUMBER> ];
        }
        segment { mode: climb;   from: (<lat>, <lon>, altitude: <N> m|ft); to: (...); }
        segment { mode: cruise;  from: (...); to: (...); }
        segment { mode: descend; from: (...); to: (...); }
    }
    // more legs...
}
```

#### Key Tokens

| Token | Format | Example |
|-------|--------|---------|
| Flight designator (`ID`) | `[A-Za-z][A-Za-z0-9]*` | `TP1234` |
| Airport code (IATA) | 3 uppercase letters | `OPO`, `MAD` |
| Airport code (ICAO) | 4 uppercase letters | `LPPR`, `LEMD` |
| Date | `YYYY-MM-DD` | `2026-05-05` |
| Time | `HH:MM` | `08:00` |
| Units (fuel) | `kg`, `l` | `40200 kg` |
| Units (altitude) | `m`, `ft` | `9249 m` |
| Units (speed) | `kts`, `m/s` | `210 kts` |

### Complete Valid Example

```
flight TP1234 {
    type:      regular;
    aircraft:  A320;
    route:     AA123 from OPO to MAD;

    leg {
        departure {
            airport: OPO;
            date:    2026-05-05;
            time:    08:00;
        }

        arrival {
            airport: MAD;
            date:    2026-05-05;
            time:    09:30;
        }

        fuel {
            quantity: 40200 kg;
        }

        profile {
            climb   [ altitude: 0 m,     speed: 210 kts ];
            climb   [ altitude: 1000 m,  speed: 210 kts ];
            descend [ altitude: 12000 m, speed: 300 kts, rate: -10 ];
            descend [ altitude: 0 m,     speed: 140 kts, rate: -5  ];
        }

        segment {
            mode: climb;
            from: (41.262891, -8.68522, altitude: 69 m);
            to:   (42.0, -8.01, altitude: 9249 m);
        }

        segment {
            mode: cruise;
            from: (42.0, -8.01, altitude: 9249 m);
            to:   (41.0, -4.3, altitude: 9249 m);
        }

        segment {
            mode: descend;
            from: (41.0, -4.3, altitude: 9249 m);
            to:   (40.4895, -3.5643, altitude: 610 m);
        }
    }
}
```

---

## Validation Pipeline

Processing is handled by `FlightDslProcessor` and runs in three sequential phases:

```
DSL Text / File
      │
      ▼
┌─────────────────────┐
│  1. Lexical Analysis │  (ANTLR FlightDSLLexer)
│     Tokenisation     │  → ParseCancellationException on invalid token
└─────────────────────┘
      │
      ▼
┌──────────────────────┐
│  2. Syntactic Analysis│  (ANTLR FlightDSLParser)
│     Parse Tree        │  → ParseCancellationException on grammar violation
└──────────────────────┘
      │
      ▼
┌──────────────────────────┐
│  3. AST Construction      │  (FlightAstBuilderVisitor)
│     + Structural Summary  │  (FlightStructureListener)
└──────────────────────────┘
      │
      ▼
┌─────────────────────────┐
│  4. Semantic Validation  │  (FlightSemanticValidator)
│     Returns SemanticError│  → List of typed errors with line/col
└─────────────────────────┘
      │
      ▼
  FlightDslProcessingResult
  (document, summary, errors)
```

### Semantic Rules

The `FlightSemanticValidator` enforces the following rules:

| Rule | Error Message |
|------|--------------|
| No duplicate flight identifiers | `Duplicate flight identifier: <ID>` |
| No airport visited more than once per flight | `Airport appears more than once in the same flight: <CODE>` |
| Each leg's arrival airport must match the next leg's departure | `Arrival airport of leg N must match departure airport of leg N+1` |
| Arrival date/time must be after departure date/time | `Arrival date/time must be after departure date/time in leg N` |
| Leg departure must be after previous leg's arrival | `Departure of leg N+1 must be after arrival of leg N` |
| Route origin must match first leg departure | `Route origin (X) must match first leg departure airport (Y)` |
| Route destination must match last leg arrival | `Route destination (X) must match last leg arrival airport (Y)` |
| Fuel quantity must be strictly positive | `Fuel quantity in leg N must be strictly positive` |
| Climb profile: altitude ≥ 0, speed > 0 | `Climb profile values must be valid and positive in leg N` |
| Descend profile: altitude ≥ 0, speed > 0, rate < 0 | `Descend profile values must be valid and rate must be negative in leg N` |
| Segment from/to coordinates must differ | `Segment start and end coordinates must differ in leg N` |
| Latitude must be in range [-90, 90] | `Latitude must be between -90 and 90 in leg N` |
| Longitude must be in range [-180, 180] | `Longitude must be between -180 and 180 in leg N` |

All errors include the **type** (`SEMANTIC`), **line**, and **column** where the problem was detected.

---

## User Interaction Flow (Console UI)

The `CreateFlightPlanUI` guides a pilot through each field interactively:

1. **Flight header** — designator, type, aircraft ID, route reference, origin, destination airport
2. **Leg(s)** — one or more legs, each with:
   - Departure (airport code, date, time)
   - Arrival (airport code, date, time)
   - Fuel (quantity and unit)
   - Optional flight profile (climb and/or descend entries)
   - Three mandatory segments: **climb**, **cruise**, **descend** (from/to coordinates with altitude)
3. **DSL preview** — the generated DSL text is printed before validation
4. **Validation** — runs lexical → syntactic → semantic analysis; errors are printed with location details
5. **JSON export** — on success, the flight plan is serialised to a JSON file (default: `aisafe-simulation/flightplans/<designator>.json`)
6. **Import confirmation** — pilot confirms before the flight plan is persisted to the database via `ImportFlightPlanController`

Only pilots (role `PILOT`) are allowed to access this feature; any other role is rejected before the form is shown.

---

## Key Classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `FlightDSL.g4` | `aisafe.flight.dsl` / antlr4 | ANTLR grammar — defines all tokens and parser rules |
| `FlightDslProcessor` | `flight.dsl.processing` | Orchestrates lexical → syntactic → AST → semantic pipeline |
| `FlightAstBuilderVisitor` | `flight.dsl.visitor` | Builds the typed AST from the parse tree |
| `FlightStructureListener` | `flight.dsl.listener` | Collects structural summary (flight count, leg count, segment count) |
| `FlightSemanticValidator` | `flight.dsl.validation` | Applies all semantic rules and returns a list of `SemanticError` |
| `FlightDslProcessingResult` | `flight.dsl.model` | Immutable result: document, summary, and error list |
| `CreateFlightPlanUI` | `airtransport.presentation.flightplan` | Interactive console form; builds DSL, validates, exports JSON, imports |
| `CreateFlightPlanAction` | `airtransport.presentation.flightplan` | Menu action wiring — delegates to `CreateFlightPlanUI.show()` |

---

## Error Output Example

Lexical/syntactic errors stop processing immediately:

```
VALIDATION FAILED — Lexical / Syntactic error:
  line 7:4 mismatched input 'departue' expecting {'departure', ...}
```

Semantic errors list all violations found:

```
VALIDATION FAILED — 2 semantic error(s):
  [SEMANTIC] line 14, col 8: Arrival date/time must be after departure date/time in leg 1
  [SEMANTIC] line 28, col 8: Fuel quantity in leg 2 must be strictly positive
```

A fully valid document produces:

```
VALID — no errors found.
  Flights: 1   Legs: 1   Segments: 3
```
