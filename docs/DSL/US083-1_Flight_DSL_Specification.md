# Flight DSL – Informal Specification

**User Story:** US083-1  
**Version:** 1.0  
**Sprint:** 2  
**Status:** Draft

---

## Table of Contents

1. [Overview](#1-overview)
2. [Conceptual Model](#2-conceptual-model)
3. [Lexical Elements](#3-lexical-elements)
4. [Informal Syntactic Structure](#4-informal-syntactic-structure)
5. [Block Reference](#5-block-reference)
6. [Valid Example](#6-valid-example)
7. [Invalid Examples and Error Cases](#7-invalid-examples-and-error-cases)
8. [Design Decisions and Notes](#8-design-decisions-and-notes)

---

## 1. Overview

The **Flight DSL** is a textual Domain Specific Language used to describe flight plans in the AISafe system. Its purpose is to allow flight plans to be defined outside the system (in a specialised tool) and then imported, validated, and processed by it — avoiding vendor lock-in with any specific internal representation.

A flight plan file written in this DSL is passed through three stages:

```
DSL File → Lexical Analysis → Syntactic Analysis → Semantic Validation → Internal Representation
```

Each stage is a gate: a file that fails lexical analysis is rejected before syntactic analysis begins, and so on. Only a fully valid file produces an internal flight plan object in the system.

The DSL is **hierarchical and block-based**. The top-level construct is a `flight` block, which contains one or more `leg` blocks. Each leg contains `departure`, `arrival`, `route`, `segment`, and `fuel` blocks.

---

## 2. Conceptual Model

### 2.1 Entity Overview

```
Flight
├── ID              (unique flight identifier)
├── Type            (regular | charter)
├── Route           (route name reference)
└── Leg [1..*]
    ├── Departure
    │   ├── Airport code  (IATA or ICAO)
    │   ├── Date
    │   └── Time
    ├── Arrival
    │   ├── Airport code  (IATA or ICAO)
    │   ├── Date
    │   └── Time
    ├── Fuel
    │   ├── Quantity      (positive number)
    │   └── Unit          (kg | l)
    ├── FlightProfile
    │   ├── Climb [0..*]
    │   │   ├── Altitude
    │   │   └── Speed
    │   └── Descend [0..*]
    │       ├── Altitude
    │       ├── Speed
    │       └── RateDescent
    └── Segment [1..*]
        ├── Mode          (climb | cruise | descend)
        ├── Start
        │   ├── Latitude
        │   ├── Longitude
        │   └── Altitude  (quantity + unit)
        └── End
            ├── Latitude
            ├── Longitude
            └── Altitude  (quantity + unit)
```

### 2.2 Entity Descriptions

| Entity | Description |
|---|---|
| **Flight** | The top-level construct. Represents a complete flight operation with a unique identifier, a type, a route reference, and one or more legs. |
| **Leg** | A single non-stop journey between two airports. A flight with intermediate stops has multiple legs. Each leg is self-contained with its own departure, arrival, fuel, profile, and segments. |
| **Departure** | The origin point of a leg: airport code, date, and scheduled time of departure. |
| **Arrival** | The destination point of a leg: airport code, date, and scheduled time of arrival. |
| **Fuel** | The fuel load assigned to a leg, expressed as a positive quantity with a unit (kg or l). |
| **FlightProfile** | Optional climb and descent profiles, describing altitude/speed tables used for simulation. Descent entries also include a rate of descent. |
| **Segment** | A linear path within a leg, characterised by a mode (climb, cruise, or descend), start and end 3D coordinates. Segments must cover the full spatial path of the leg. |

### 2.3 Key Constraints (Conceptual Level)

- A `flight` must have at least one `leg`.
- Each `leg` must have exactly one `departure`, one `arrival`, one `fuel` block, and at least one `segment`.
- The `FlightProfile` is optional but, if present, must contain at least one `climb` or one `descend` entry.
- The arrival airport of leg *n* must equal the departure airport of leg *n+1* (leg sequence coherence).
- The arrival time of leg *n* must be strictly before the departure time of leg *n+1*.
- The route origin must match the first leg's departure airport.
- The route destination must match the last leg's arrival airport.
- No airport may appear twice across all legs of the same flight.
- Segment start and end coordinates must differ.

---

## 3. Lexical Elements

### 3.1 Case Sensitivity Rules

| Category | Case Sensitivity |
|---|---|
| Keywords | **Case-insensitive** (`FLIGHT`, `flight`, `Flight` are all valid) |
| Identifiers | **Case-sensitive** (`TP123` ≠ `tp123`) |
| Airport codes | **Case-sensitive** (IATA codes are uppercase by convention: `OPO`, `MAD`) |
| String literals | **Case-sensitive** |
| Numeric literals | Not applicable |

### 3.2 Token Categories

#### Keywords

Reserved words that define the structure of the language. They may not be used as identifiers.

| Keyword | Purpose |
|---|---|
| `flight` | Opens a flight block |
| `leg` | Opens a leg block |
| `departure` | Opens a departure block |
| `arrival` | Opens an arrival block |
| `route` | Opens a route block |
| `segment` | Opens a segment block |
| `fuel` | Opens a fuel block |
| `profile` | Opens a flight profile block |
| `climb` | Opens a climb entry or marks a segment mode |
| `cruise` | Marks a segment mode |
| `descend` | Opens a descend entry or marks a segment mode |
| `regular` | Flight type value |
| `charter` | Flight type value |
| `type` | Introduces the flight type field |
| `from` | Introduces a segment start coordinate |
| `to` | Introduces a segment end coordinate |
| `altitude` | Introduces an altitude field |
| `speed` | Introduces a speed field |
| `rate` | Introduces a rate of descent field |
| `mode` | Introduces a segment mode field |
| `quantity` | Introduces a fuel quantity field |
| `date` | Introduces a date field |
| `time` | Introduces a time field |

#### Identifiers

Used for the flight ID and the route name reference. Must begin with a letter and may contain letters and digits.

```
Pattern: [A-Za-z][A-Za-z0-9]*
Examples: TP1234, AA123, myFlight
```

> **Note:** The flight designator format (e.g. `TP1234A`) is a specific type of identifier: two letters (airline designator), followed by 1–4 digits, and an optional one-letter operational suffix. Semantic validation will enforce this constraint.

#### Airport Codes

Either IATA (3-letter) or ICAO (4-letter) uppercase codes.

```
IATA pattern: [A-Z]{3}           Examples: OPO, MAD, LIS
ICAO pattern: [A-Z]{4}           Examples: LPPR, LEMD, LPPT
```

#### Numeric Literals

Integers and floating-point numbers. Negative values are only valid where explicitly permitted (e.g. rate of descent).

```
Integer:  [0-9]+                 Examples: 0, 100, 35000
Float:    [0-9]+ '.' [0-9]+      Examples: 41.262891, 9249.0, 0.804
```

#### Date Literals

ISO 8601 date format.

```
Pattern: YYYY-MM-DD              Examples: 2026-05-05, 2026-12-31
```

#### Time Literals

24-hour clock format.

```
Pattern: HH:MM                   Examples: 14:30, 00:00, 23:59
```

#### Units

Qualify numeric values. Must follow the number they modify.

| Unit token | Meaning | Applicable fields |
|---|---|---|
| `kg` | kilograms | Fuel quantity |
| `l` | litres | Fuel quantity |
| `m` | metres | Altitude |
| `ft` | feet | Altitude (alternative) |
| `m/s` | metres per second | Wind speed |
| `kts` | knots | Speed |

#### Separators and Symbols

| Symbol | Usage |
|---|---|
| `{` `}` | Open/close a block |
| `(` `)` | Coordinate groupings |
| `[` `]` | Profile entry groupings |
| `:` | Separates a field name from its value |
| `;` | Terminates a field or entry |
| `,` | Separates values within a coordinate |

#### Whitespace and Comments

Whitespace (spaces, tabs, newlines) is freely allowed between tokens and carries no semantic meaning — it exists only to aid readability.

Single-line comments using `//` are supported and ignored by the lexer:

```
// This is a comment
```

---

## 4. Informal Syntactic Structure

### 4.1 Top-Level Structure

A valid DSL file contains exactly one `flight` block.

```
flight <ID> {
    type: <flight-type>;
    route: <route-name>;

    leg { ... }
    leg { ... }   // one or more legs
}
```

### 4.2 Leg Block

Each leg represents one non-stop flight phase between two airports.

```
leg {
    departure { ... }
    arrival   { ... }
    fuel      { ... }
    profile   { ... }   // optional
    segment   { ... }   // one or more segments
}
```

### 4.3 Departure and Arrival Blocks

```
departure {
    airport: <airport-code>;
    date:    <YYYY-MM-DD>;
    time:    <HH:MM>;
}

arrival {
    airport: <airport-code>;
    date:    <YYYY-MM-DD>;
    time:    <HH:MM>;
}
```

### 4.4 Fuel Block

```
fuel {
    quantity: <positive-number> <unit>;
}
```

### 4.5 Flight Profile Block (Optional)

```
profile {
    climb   [ altitude: <number> <unit>, speed: <number> <unit> ];  // 0 or more
    descend [ altitude: <number> <unit>, speed: <number> <unit>, rate: <number> ];  // 0 or more
}
```

### 4.6 Segment Block

```
segment {
    mode: <climb | cruise | descend>;
    from: (<latitude>, <longitude>, altitude: <number> <unit>);
    to:   (<latitude>, <longitude>, altitude: <number> <unit>);
}
```

### 4.7 Full Structure Diagram

```
flight
├── type
├── route
└── leg [1..*]
    ├── departure
    │   ├── airport
    │   ├── date
    │   └── time
    ├── arrival
    │   ├── airport
    │   ├── date
    │   └── time
    ├── fuel
    │   └── quantity <unit>
    ├── profile [0..1]
    │   ├── climb [0..*]   → altitude, speed
    │   └── descend [0..*] → altitude, speed, rate
    └── segment [1..*]
        ├── mode
        ├── from → (lat, lon, altitude)
        └── to   → (lat, lon, altitude)
```

---

## 5. Block Reference

A condensed reference of every block and its mandatory vs. optional fields.

### `flight` block

| Field | Type | Mandatory | Description |
|---|---|---|---|
| `type` | keyword (`regular`\|`charter`) | Yes | Type of flight |
| `route` | identifier | Yes | Route name reference |
| `leg` | block | Yes (1 or more) | One leg per non-stop journey segment |

### `leg` block

| Field | Type | Mandatory | Description |
|---|---|---|---|
| `departure` | block | Yes | Departure information |
| `arrival` | block | Yes | Arrival information |
| `fuel` | block | Yes | Fuel load for this leg |
| `profile` | block | No | Altitude/speed climb and descent tables |
| `segment` | block | Yes (1 or more) | Spatial segments of the leg |

### `departure` / `arrival` blocks

| Field | Type | Mandatory | Description |
|---|---|---|---|
| `airport` | airport code | Yes | IATA or ICAO airport code |
| `date` | date literal | Yes | Date in `YYYY-MM-DD` format |
| `time` | time literal | Yes | Time in `HH:MM` format |

### `fuel` block

| Field | Type | Mandatory | Description |
|---|---|---|---|
| `quantity` | positive number + unit | Yes | Fuel load (`kg` or `l`) |

### `profile` block

| Field | Type | Mandatory | Description |
|---|---|---|---|
| `climb` | entry list | No | One or more climb profile entries |
| `descend` | entry list | No | One or more descend profile entries |

Each `climb` entry: `altitude` (number + unit), `speed` (number + unit).  
Each `descend` entry: `altitude` (number + unit), `speed` (number + unit), `rate` (negative number).

### `segment` block

| Field | Type | Mandatory | Description |
|---|---|---|---|
| `mode` | keyword (`climb`\|`cruise`\|`descend`) | Yes | Phase of flight for this segment |
| `from` | coordinate tuple | Yes | Start point (lat, lon, altitude) |
| `to` | coordinate tuple | Yes | End point (lat, lon, altitude) |

---

## 6. Valid Example

This example is derived from the JSON reference (`Flight_Plan_v0a.json`) and represents a single-leg regular flight from Porto (OPO) to Madrid (MAD).

```
// Flight plan: OPO → MAD
// Regular flight TP1234, route AA123

flight TP1234 {
    type:  regular;
    route: AA123;

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
            climb   [ altitude: 2000 m,  speed: 220 kts ];
            climb   [ altitude: 5000 m,  speed: 260 kts ];
            climb   [ altitude: 10000 m, speed: 300 kts ];
            climb   [ altitude: 12000 m, speed: 300 kts ];

            descend [ altitude: 12000 m, speed: 300 kts, rate: -10 ];
            descend [ altitude: 5000 m,  speed: 290 kts, rate: -10 ];
            descend [ altitude: 2000 m,  speed: 250 kts, rate: -7  ];
            descend [ altitude: 1000 m,  speed: 180 kts, rate: -7  ];
            descend [ altitude: 0 m,     speed: 140 kts, rate: -5  ];
        }

        // Segment 1: climb from OPO elevation to cruising altitude
        segment {
            mode: climb;
            from: (41.262891, -8.68522,  altitude: 69 m);
            to:   (42.0,      -8.01,     altitude: 9249 m);
        }

        // Segment 2: cruise at constant altitude
        segment {
            mode: cruise;
            from: (42.0,    -8.01, altitude: 9249 m);
            to:   (41.0,    -4.3,  altitude: 9249 m);
        }

        // Segment 3: descend toward MAD
        segment {
            mode: descend;
            from: (41.0,    -4.3,    altitude: 9249 m);
            to:   (40.4895, -3.5643, altitude: 610 m);
        }

    }
}
```

---

## 7. Invalid Examples and Error Cases

Each example below corresponds to a specific validation rule. These will serve as negative test cases for the ANTLR grammar and semantic validator.

---

### 7.1 Missing `fuel` block — Syntactic Error

```
// ERROR: leg is missing the mandatory fuel block

flight TP1234 {
    type:  regular;
    route: AA123;

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
        segment {
            mode: cruise;
            from: (42.0, -8.01, altitude: 9249 m);
            to:   (41.0, -4.3,  altitude: 9249 m);
        }
    }
}
```
**Expected error:** Syntactic — missing mandatory `fuel` block inside `leg`.

---

### 7.2 Missing `segment` block — Syntactic Error

```
// ERROR: leg must have at least one segment

flight TP1234 {
    type:  regular;
    route: AA123;

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
        // No segment blocks here
    }
}
```
**Expected error:** Syntactic — `leg` must contain at least one `segment`.

---

### 7.3 Arrival Airport Mismatch Between Legs — Semantic Error

```
// ERROR: arrival of leg 1 (MAD) does not match departure of leg 2 (LIS)

flight TP5678 {
    type:  regular;
    route: TP001;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        segment {
            mode: cruise;
            from: (41.26, -8.68, altitude: 69 m);
            to:   (40.49, -3.56, altitude: 610 m);
        }
    }

    leg {
        departure { airport: LIS; date: 2026-05-05; time: 11:00; }
        arrival   { airport: BCN; date: 2026-05-05; time: 13:00; }
        fuel { quantity: 38000 kg; }
        segment {
            mode: cruise;
            from: (38.77, -9.13, altitude: 114 m);
            to:   (41.29,  2.07, altitude: 4 m);
        }
    }
}
```
**Expected error:** Semantic — arrival airport of leg 1 (`MAD`) must equal departure airport of leg 2 (`LIS`).

---

### 7.4 Negative Fuel Quantity — Semantic Error

```
// ERROR: fuel quantity must be strictly positive

flight TP1234 {
    type:  regular;
    route: AA123;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel {
            quantity: -500 kg;    // invalid: must be > 0
        }
        segment {
            mode: cruise;
            from: (41.26, -8.68, altitude: 69 m);
            to:   (40.49, -3.56, altitude: 610 m);
        }
    }
}
```
**Expected error:** Semantic — fuel quantity must be strictly positive.

---

### 7.5 Invalid Flight Type — Semantic Error

```
// ERROR: "private" is not a valid flight type

flight TP1234 {
    type:  private;    // invalid: must be regular or charter
    route: AA123;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        segment {
            mode: cruise;
            from: (41.26, -8.68, altitude: 69 m);
            to:   (40.49, -3.56, altitude: 610 m);
        }
    }
}
```
**Expected error:** Semantic — flight type must be one of `regular` or `charter`.

---

### 7.6 Segment Start Equals End — Semantic Error

```
// ERROR: segment start and end coordinates are identical

flight TP1234 {
    type:  regular;
    route: AA123;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        segment {
            mode: cruise;
            from: (41.26, -8.68, altitude: 9249 m);
            to:   (41.26, -8.68, altitude: 9249 m);    // identical to from
        }
    }
}
```
**Expected error:** Semantic — segment `from` and `to` coordinates must differ.

---

### 7.7 Duplicate Airport in Flight — Semantic Error

```
// ERROR: OPO appears as both the origin and an intermediate stop

flight TP9999 {
    type:  regular;
    route: XX001;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        segment {
            mode: cruise;
            from: (41.26, -8.68, altitude: 69 m);
            to:   (40.49, -3.56, altitude: 610 m);
        }
    }

    leg {
        departure { airport: MAD; date: 2026-05-05; time: 11:00; }
        arrival   { airport: OPO; date: 2026-05-05; time: 12:30; }  // OPO visited twice
        fuel { quantity: 38000 kg; }
        segment {
            mode: cruise;
            from: (40.49, -3.56, altitude: 610 m);
            to:   (41.26, -8.68, altitude: 69 m);
        }
    }
}
```
**Expected error:** Semantic — the same airport (`OPO`) cannot be visited twice in a flight.

---

### 7.8 Malformed Date — Lexical Error

```
// ERROR: date is not in YYYY-MM-DD format

flight TP1234 {
    type:  regular;
    route: AA123;

    leg {
        departure {
            airport: OPO;
            date:    05-05-2026;    // wrong format — must be YYYY-MM-DD
            time:    08:00;
        }
        ...
    }
}
```
**Expected error:** Lexical — date token does not match the expected pattern `YYYY-MM-DD`.

---

## 8. Design Decisions and Notes

### 8.1 Why block-based syntax?

A hierarchical, block-delimited syntax (`{ }`) was chosen because:
- It is self-evidently structured and readable by humans.
- It maps cleanly onto ANTLR parser rules (each block becomes a rule).
- It scales naturally to multi-leg flights without ambiguity.
- It is consistent with widely adopted configuration DSLs (HCL, JSON, etc.).

### 8.2 Why support both IATA and ICAO codes?

The requirements explicitly state that airport codes may be IATA (3-letter) or ICAO (4-letter). The lexer distinguishes them by length; semantic validation will check them against registered airports in the system.

### 8.3 Why is `profile` optional?

The requirements describe the flight profile (climb/descent tables) as contextual data for simulation. Not all flight plans will specify it at creation time, and the requirements do not mandate it as a core DSL field. Making it optional keeps the DSL compatible with both simple and detailed flight plans.

### 8.4 Relationship to the ANTLR grammar (next sub-issue)

This document is the design input for `FlightDSL.g4`. The mapping will be:

| DSL concept | ANTLR artefact |
|---|---|
| Keywords (e.g. `flight`, `leg`) | Lexer rules with character-class alternation for case-insensitivity |
| Identifiers | `ID` lexer rule |
| Airport codes | `IATA_CODE` / `ICAO_CODE` lexer rules |
| Numeric literals | `INT` / `FLOAT` lexer rules |
| Date / Time literals | `DATE` / `TIME` lexer rules |
| Block structure | Parser rules (`flightPlan`, `leg`, `departure`, etc.) |
| Semantic constraints (e.g. leg coherence) | Visitor or Listener walking the parse tree |

### 8.5 Error reporting

All three validation stages must produce clear, actionable errors:

- **Lexical errors** — unrecognised character or malformed token. Must report line and column.
- **Syntactic errors** — missing or unexpected token. Must report line, column, and what was expected.
- **Semantic errors** — structurally valid but domain-invalid content (e.g. fuel ≤ 0). Must report which rule was violated and where.

All semantic errors must be **collected and reported together** in a single execution pass — the validator must not abort on the first error.

---

*This document will be updated if the DSL is extended beyond the Core specification during Sprint 2.*
