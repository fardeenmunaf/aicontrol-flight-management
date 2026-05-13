# Flight DSL – Informal Specification

**User Story:** US083-1  
**Version:** 1.1  
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

The DSL is **hierarchical and block-based**. The top-level construct is a `flight` block, which contains one or more `leg` blocks. Each leg contains `departure`, `arrival`, `fuel`, `segment`, and an optional `profile` block.

A DSL file may contain **one or more `flight` blocks**. All flight identifiers within the same file must be unique.

---

## 2. Conceptual Model

### 2.1 Entity Overview

```
Flight
├── ID              (unique flight identifier within the file)
├── Type            (regular | charter)
├── Route
│   ├── Reference   (route name identifier)
│   ├── Origin      (IATA or ICAO airport code)
│   └── Destination (IATA or ICAO airport code)
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
    │   ├── Quantity      (strictly positive number)
    │   └── Unit          (kg | l)
    ├── FlightProfile [0..1]
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
| **Fuel** | The fuel load assigned to a leg, expressed as a strictly positive quantity with a unit (kg or l). |
| **FlightProfile** | Optional climb and descent profiles, describing altitude/speed tables used for simulation. Descent entries also include a rate of descent. |
| **Segment** | A linear path within a leg, characterised by a mode (climb, cruise, or descend), start and end 3D coordinates. Segments must cover the full spatial path of the leg. |

### 2.3 Key Constraints (Conceptual Level)

- A DSL file must contain at least one `flight` block; all flight identifiers must be unique within the file.
- A `flight` must have at least one `leg`.
- Each `leg` must have exactly one `departure`, one `arrival`, one `fuel` block, and at least one `segment`.
- The `FlightProfile` is optional but, if present, must contain at least one `climb` or one `descend` entry.
- The arrival airport of leg *n* must equal the departure airport of leg *n+1* (leg sequence coherence).
- The arrival time of leg *n* must be strictly before the departure time of leg *n+1*.
- The route origin must match the first leg's departure airport.
- The route destination must match the last leg's arrival airport.
- No airport may appear twice across all legs of the same flight.
- Segment start and end coordinates must differ.
- Fuel quantity must be strictly positive (greater than zero).

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
| `route` | Introduces the route declaration |
| `segment` | Opens a segment block |
| `fuel` | Opens a fuel block |
| `profile` | Opens a flight profile block |
| `climb` | Opens a climb entry or marks a segment mode |
| `cruise` | Marks a segment mode |
| `descend` | Opens a descend entry or marks a segment mode |
| `regular` | Flight type value |
| `charter` | Flight type value |
| `type` | Introduces the flight type field |
| `airport` | Introduces an airport code field |
| `from` | Introduces the route origin airport or a segment start coordinate |
| `to` | Introduces the route destination airport or a segment end coordinate |
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

Integers and floating-point numbers. Negative values are only valid where explicitly permitted (e.g. rate of descent, coordinates).

```
Integer:  [0-9]+                 Examples: 0, 100, 35000
Float:    [0-9]+ '.' [0-9]+      Examples: 41.262891, 9249.0, 0.804
```

> **Note:** Fuel quantity and altitude use the grammar rule `positiveNumber` (INT | FLOAT, no minus sign). A value of `0` passes parsing but fuel quantity `0` is rejected at the semantic level. Coordinates and descent rates use `signedNumber` and may be negative.

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
| `,` | Separates values within a coordinate or profile entry |

#### Whitespace and Comments

Whitespace (spaces, tabs, newlines) is freely allowed between tokens and carries no semantic meaning — it exists only to aid readability.

Single-line comments using `//` are supported and ignored by the lexer:

```
// This is a comment
```

---

## 4. Informal Syntactic Structure

### 4.1 Top-Level Structure

A valid DSL file contains **one or more `flight` blocks**. All flight identifiers must be unique within the file.

```
flight <ID> {
    type:  <flight-type>;
    route: <route-name> from <airport-code> to <airport-code>;

    leg { ... }
    leg { ... }   // one or more legs
}
```

The `route` declaration includes:
- `<route-name>` — an identifier referencing the route in the system
- `from <airport-code>` — the route's origin airport (must match the first leg's departure airport)
- `to <airport-code>` — the route's destination airport (must match the last leg's arrival airport)

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

The quantity must be strictly greater than zero. A value of `0` is syntactically valid but rejected during semantic validation.

### 4.5 Flight Profile Block (Optional)

```
profile {
    climb   [ altitude: <number> <unit>, speed: <number> <unit> ];  // 0 or more
    descend [ altitude: <number> <unit>, speed: <number> <unit>, rate: <signed-number> ];  // 0 or more
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
├── route <name> from <origin> to <destination>
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
| `route` | identifier + `from` airport + `to` airport | Yes | Route reference with explicit origin and destination airports |
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
| `quantity` | positive number + unit | Yes | Fuel load (`kg` or `l`); must be strictly greater than zero |

### `profile` block

| Field | Type | Mandatory | Description |
|---|---|---|---|
| `climb` | entry list | No | One or more climb profile entries |
| `descend` | entry list | No | One or more descend profile entries |

Each `climb` entry: `altitude` (number + unit), `speed` (number + unit, must be > 0).  
Each `descend` entry: `altitude` (number + unit), `speed` (number + unit, must be > 0), `rate` (must be negative).

### `segment` block

| Field | Type | Mandatory | Description |
|---|---|---|---|
| `mode` | keyword (`climb`\|`cruise`\|`descend`) | Yes | Phase of flight for this segment |
| `from` | coordinate tuple | Yes | Start point (lat, lon, altitude) |
| `to` | coordinate tuple | Yes | End point (lat, lon, altitude); must differ from `from` |

---

## 6. Valid Example

This example is derived from the JSON reference (`Flight_Plan_v0a.json`) and represents a single-leg regular flight from Porto (OPO) to Madrid (MAD).

```
// Flight plan: OPO → MAD
// Regular flight TP1234, route AA123

flight TP1234 {
    type:  regular;
    route: AA123 from OPO to MAD;

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

Each example below corresponds to a specific validation rule and serves as a negative test case for the ANTLR grammar and semantic validator.

> **Note on error types:**
> - **Lexical error** — unrecognised character or malformed token (e.g. wrong date format).
> - **Syntactic error** — token stream violates grammar rules (e.g. missing mandatory block, unrecognised keyword). The grammar itself catches these before semantic validation.
> - **Semantic error** — syntactically valid but domain-invalid (e.g. duplicate airport, fuel = 0).

---

### 7.1 Missing `fuel` block — Syntactic Error

```
// ERROR: leg is missing the mandatory fuel block

flight TP1234 {
    type:  regular;
    route: AA123 from OPO to MAD;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
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
    route: AA123 from OPO to MAD;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
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
    route: TP001 from OPO to BCN;

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

### 7.4 Negative Fuel Quantity — Syntactic Error

```
// ERROR: positiveNumber does not allow a minus sign

flight TP1234 {
    type:  regular;
    route: AA123 from OPO to MAD;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel {
            quantity: -500 kg;    // invalid: positiveNumber = INT | FLOAT (no minus)
        }
        segment {
            mode: cruise;
            from: (41.26, -8.68, altitude: 69 m);
            to:   (40.49, -3.56, altitude: 610 m);
        }
    }
}
```
**Expected error:** Syntactic — the grammar rule `positiveNumber` does not accept a minus sign; `-500` is an unexpected token.

---

### 7.5 Invalid Flight Type — Syntactic Error

```
// ERROR: "private" is not a valid flight type

flight TP1234 {
    type:  private;    // invalid: grammar only accepts "regular" or "charter"
    route: AA123 from OPO to MAD;

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
**Expected error:** Syntactic — the parser rule `flightType` only matches `regular` or `charter`; any other word is an unexpected token.

---

### 7.6 Segment Start Equals End — Semantic Error

```
// ERROR: segment start and end coordinates are identical

flight TP1234 {
    type:  regular;
    route: AA123 from OPO to MAD;

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
// ERROR: OPO appears twice (departure of leg 1 and arrival of leg 2)

flight TP9999 {
    type:  regular;
    route: XX001 from OPO to OPO;

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
    route: AA123 from OPO to MAD;

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

### 7.9 Duplicate Flight Identifier — Semantic Error

```
// ERROR: TP1234 used as flight identifier in two separate flight blocks

flight TP1234 {
    type:  regular;
    route: AA123 from OPO to MAD;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        segment { mode: cruise; from: (41.26, -8.68, altitude: 69 m); to: (40.49, -3.56, altitude: 610 m); }
    }
}

flight TP1234 {    // duplicate identifier
    type:  charter;
    route: BB456 from LIS to BCN;

    leg {
        departure { airport: LIS; date: 2026-05-06; time: 10:00; }
        arrival   { airport: BCN; date: 2026-05-06; time: 12:00; }
        fuel { quantity: 35000 kg; }
        segment { mode: cruise; from: (38.77, -9.13, altitude: 114 m); to: (41.29, 2.07, altitude: 4 m); }
    }
}
```
**Expected error:** Semantic — flight identifier `TP1234` appears more than once in the same file.

---

### 7.10 Route Origin/Destination Mismatch — Semantic Error

```
// ERROR: route declares origin LIS and destination BCN,
//        but leg departs OPO and arrives MAD

flight TP7777 {
    type:  regular;
    route: RP001 from LIS to BCN;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }  // OPO ≠ LIS
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }  // MAD ≠ BCN
        fuel { quantity: 40200 kg; }
        segment { mode: cruise; from: (41.26, -8.68, altitude: 69 m); to: (40.49, -3.56, altitude: 610 m); }
    }
}
```
**Expected errors:** Semantic — route origin (`LIS`) must match first leg departure (`OPO`); route destination (`BCN`) must match last leg arrival (`MAD`).

---

### 7.11 Zero Fuel Quantity — Semantic Error

```
// ERROR: 0 passes the parser (valid positiveNumber) but is rejected semantically

flight TP1234 {
    type:  regular;
    route: AA123 from OPO to MAD;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 0 kg; }
        segment { mode: cruise; from: (41.26, -8.68, altitude: 69 m); to: (40.49, -3.56, altitude: 610 m); }
    }
}
```
**Expected error:** Semantic — fuel quantity must be strictly greater than zero.

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

### 8.4 Why does `route` declare origin and destination explicitly?

The route declaration (`route: AA123 from OPO to MAD;`) embeds the origin and destination airports inline. This makes the DSL file **self-contained and self-validating**: the semantic validator can immediately check that the declared endpoints match the first and last leg airports (AC10) without requiring a database lookup during parsing.

### 8.5 Why are negative fuel and invalid flight type syntactic errors?

The grammar rule `positiveNumber` accepts only `INT` or `FLOAT` (no minus sign), so a negative fuel value is rejected at the parser before semantic validation begins. Similarly, the parser rule `flightType` is restricted to `regular` and `charter`, so any other keyword is a syntactic mismatch.

Zero fuel (`0 kg`) is different: `0` is a valid `INT` and a valid `positiveNumber`, so it passes parsing but is rejected by the semantic validator which checks `quantity > 0`.

### 8.6 Why allow multiple `flight` blocks in one file?

A single DSL file may describe a batch of flight plans (e.g. a day's schedule). The grammar allows `flightPlan+` (one or more). The uniqueness constraint on flight identifiers (AC08) ensures each flight is unambiguously identifiable within the file.

### 8.7 Relationship to the ANTLR grammar

| DSL concept | ANTLR artefact |
|---|---|
| Keywords (e.g. `flight`, `leg`) | Lexer rules with character-class alternation for case-insensitivity |
| Identifiers | `ID` lexer rule |
| Airport codes | `IATA_CODE` / `ICAO_CODE` lexer rules |
| Numeric literals | `INT` / `FLOAT` lexer rules |
| Date / Time literals | `DATE_LITERAL` / `TIME_LITERAL` lexer rules |
| Block structure | Parser rules (`flightPlan`, `leg`, `departure`, etc.) |
| Route origin/destination | Inline `airportCode` references in the `flightPlan` parser rule |
| Semantic constraints | `FlightSemanticValidator` traversing the AST built by `FlightAstBuilderVisitor` |

### 8.8 Error reporting

All three validation stages must produce clear, actionable errors:

- **Lexical errors** — unrecognised character or malformed token. Must report line and column.
- **Syntactic errors** — missing or unexpected token. Must report line, column, and what was expected.
- **Semantic errors** — structurally valid but domain-invalid content (e.g. fuel = 0, duplicate ID, route mismatch). Must report which rule was violated and where.

All semantic errors must be **collected and reported together** in a single execution pass — the validator must not abort on the first error.

---

*This document will be updated if the DSL is extended beyond the Core specification during Sprint 2.*
