# Flight DSL – Informal Specification

**User Story:** US083-1 / US120  
**Version:** 1.4  
**Sprint:** 4  
**Status:** Final

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

The DSL is **hierarchical and block-based**. The top-level construct is a `flight` block, which contains one or more `leg` blocks. Each leg contains `departure`, `arrival`, `fuel`, a mandatory `profile` block, and exactly three `segment` blocks in fixed order: `climb`, `cruise`, `descend`.

A DSL file may contain **one or more `flight` blocks**. All flight identifiers within the same file must be unique.

---

## 2. Conceptual Model

### 2.1 Entity Overview

```
Flight
├── ID              (flight designator: 2-3 uppercase letters + 1–4 digits + optional suffix letter)
├── Type            (regular | charter)
├── Aircraft        (aircraft registration identifier)
├── Pilot           (email address of the responsible pilot)
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
    ├── FlightProfile [1..1]  (mandatory)
    │   ├── Climb [1..*]
    │   │   ├── Altitude
    │   │   ├── Speed
    │   │   └── RateClimb    (rate of climb)
    │   ├── Cruise [1..*]
    │   │   └── Speed
    │   └── Descend [1..*]
    │       ├── Altitude
    │       ├── Speed
    │       └── RateDescent  (must be negative)
    ├── Segment: climb    (exactly 1, must appear first)
    │   ├── Start / End   (3D coordinates; end altitude must be > start altitude)
    │   ├── AltitudeSlot [1..*]
    │   │   ├── Min       (quantity + unit, >= 0)
    │   │   ├── Max       (quantity + unit, > min)
    │   │   └── Width     (quantity + unit, > 0)
    │   └── Wind
    │       ├── Direction (degrees, 0–359)
    │       └── Speed     (quantity m/s, > 0, <= 25 m/s)
    ├── Segment: cruise   (exactly 1, must appear second)
    │   ├── Start / End   (3D coordinates; from and to altitude must be equal)
    │   ├── AltitudeSlot [1..*], Wind  (same structure)
    └── Segment: descend  (exactly 1, must appear third)
        ├── Start / End   (3D coordinates; end altitude must be < start altitude)
        └── AltitudeSlot [1..*], Wind  (same structure)
```

### 2.2 Entity Descriptions

| Entity | Description |
|---|---|
| **Flight** | The top-level construct. Represents a complete flight operation with a unique designator, a type, an aircraft assignment, a pilot reference, a route reference, and one or more legs. |
| **Aircraft** | The registration identifier of the aircraft assigned to the flight. Accepts complex registration formats including hyphens and digits (e.g. `CS-TNA`, `A320`). |
| **Pilot** | The email address of the pilot responsible for this flight plan. Validated against standard email format during semantic validation. |
| **Leg** | A single non-stop journey between two airports. A flight with intermediate stops has multiple legs. Each leg is self-contained with its own departure, arrival, fuel, profile, and segments. |
| **Departure** | The origin point of a leg: airport code, date, and scheduled time of departure. The date and time must not be in the past. |
| **Arrival** | The destination point of a leg: airport code, date, and scheduled time of arrival. Must be strictly after the departure date/time of the same leg. |
| **Fuel** | The fuel load assigned to a leg, expressed as a strictly positive quantity with a unit (kg or l). |
| **FlightProfile** | Mandatory climb, cruise, and descent profiles, describing altitude/speed tables used for simulation. Climb entries must be in ascending altitude order and include a rate of climb; descent entries must be in descending altitude order and include a rate of descent (must be negative). |
| **Segment** | A linear path within a leg. Every leg must contain exactly three segments in fixed order: `climb`, `cruise`, `descend`. Each segment has a start/end 3D coordinate pair, one or more altitude slots, and a wind block. The mode is determined by position and enforced syntactically. Segment end coordinates must match the start coordinates of the next segment. |
| **AltitudeSlot** | Defines the usable vertical band for a segment: minimum altitude, maximum altitude, and slot width. Used by the simulation to assign altitude clearances. |
| **Wind** | Wind conditions at the segment: direction in degrees (0–359) and speed in m/s (strictly positive, maximum 25 m/s). |

### 2.3 Key Constraints (Conceptual Level)

- A DSL file must contain at least one `flight` block; all flight identifiers must be unique within the file.
- A flight identifier must follow the format: two or three uppercase letters (airline designator), followed by 1–4 digits, and an optional single uppercase letter suffix (e.g. `TP1234`, `TAP123`, `BA100A`). This is enforced at the lexer level.
- A `flight` must have at least one `leg`.
- Each `leg` must have exactly one `departure`, one `arrival`, one `fuel` block, one mandatory `profile` block, and exactly three `segment` blocks in fixed order: `climb`, `cruise`, `descend`.
- Each `segment` must have at least one `altslot` and exactly one `wind` block. The mode (`climb`, `cruise`, `descend`) is fixed per position and enforced syntactically.
- For each `altslot`: `min >= 0`, `max >= 0`, `min < max`, and `width > 0`.
- For each `wind`: `direction` in [0, 359] and `speed > 0` and `speed <= 25 m/s`.
- The departure date/time must not be in the past.
- Arrival date/time must be strictly after departure date/time within the same leg.
- Departure time of leg *n+1* must be strictly after arrival time of leg *n*.
- Every `leg` must contain exactly one mandatory `profile` block. The profile must contain entries for all three segment modes: at least one `climb`, `cruise`, and `descend` entry.
- The minimum altitude among profile `climb` entries must match the climb segment's `from` altitude; the maximum must match the `to` altitude.
- The maximum altitude among profile `descend` entries must match the descend segment's `from` altitude; the minimum must match the `to` altitude.
- Profile `climb` entries must be in strictly ascending altitude order.
- Profile `descend` entries must be in strictly descending altitude order.
- Descent rate values must be strictly negative.
- The climb segment's `to` altitude must be strictly greater than its `from` altitude.
- The cruise segment's `from` and `to` altitudes must be equal (constant cruise altitude).
- The descend segment's `to` altitude must be strictly less than its `from` altitude.
- Segment end coordinates must match the start coordinates of the immediately following segment (no teleportation between segments).
- Coordinate latitude must be in [-90, 90] and longitude in [-180, 180].
- Altitude must be within physical bounds: at most 13,700 m / 45,000 ft; at least -500 m / -1,640 ft.
- The arrival airport of leg *n* must equal the departure airport of leg *n+1* (leg sequence coherence).
- The route origin must match the first leg's departure airport.
- The route destination must match the last leg's arrival airport.
- No airport may appear twice across all legs of the same flight.
- Segment start and end coordinates must differ.
- Fuel quantity must be strictly positive (greater than zero).
- The `pilot` field must be a valid email address.

---

## 3. Lexical Elements

### 3.1 Case Sensitivity Rules

| Category | Case Sensitivity |
|---|---|
| Keywords | **Case-insensitive** (`FLIGHT`, `flight`, `Flight` are all valid) |
| Flight designators | **Case-sensitive** (must be uppercase: `TP1234`, `TAP123`) |
| Identifiers | **Case-sensitive** (`AA123` ≠ `aa123`) |
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
| `altslot` | Opens an altitude slot entry |
| `wind` | Opens a wind block |
| `fuel` | Opens a fuel block |
| `profile` | Opens a flight profile block |
| `climb` | Opens a climb entry or marks a segment mode |
| `cruise` | Opens a cruise entry in a profile block, or marks a segment mode |
| `descend` | Opens a descend entry or marks a segment mode |
| `regular` | Flight type value |
| `charter` | Flight type value |
| `type` | Introduces the flight type field |
| `aircraft` | Introduces the aircraft registration field |
| `pilot` | Introduces the pilot email field |
| `airport` | Introduces an airport code field |
| `from` | Introduces the route origin airport or a segment start coordinate |
| `to` | Introduces the route destination airport or a segment end coordinate |
| `altitude` | Introduces an altitude field |
| `speed` | Introduces a speed field |
| `rate` | Introduces a rate of climb or rate of descent field |
| `mode` | Introduces a segment mode field |
| `direction` | Introduces a wind direction field |
| `min` | Introduces the minimum altitude of an altitude slot |
| `max` | Introduces the maximum altitude of an altitude slot |
| `width` | Introduces the slot width of an altitude slot |
| `quantity` | Introduces a fuel quantity field |
| `date` | Introduces a date field |
| `time` | Introduces a time field |

#### Flight Designator

The flight identifier has a dedicated lexer rule that enforces the standard airline designator format at the token level — malformed designators are rejected before parsing begins.

```
Pattern: [A-Z][A-Z][A-Z]?[0-9][0-9]?[0-9]?[0-9]?[A-Z]?
Examples: TP1234, TAP123, BA100A, AF007, QR1
```

Two or three uppercase letters (airline designator code — IATA uses 2, ICAO uses 3), followed by 1–4 digits (flight number), and an optional single uppercase letter suffix (operational variant).

#### Identifiers

Used for aircraft registration and route name references. Must begin with a letter and may contain letters and digits.

```
Pattern: [A-Za-z][A-Za-z0-9]*
Examples: A320, myRoute
```

#### Aircraft Registration

Aircraft identifiers are more permissive than plain identifiers — they can include hyphens, digits, and airport code patterns (e.g. `CS-TNA`, `A320`, `B747`). The parser rule accepts one or more tokens from: `ID`, `INT`, `-`, `IATA_CODE`, `ICAO_CODE`, or `FLIGHT_DESIGNATOR`.

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

> **Note:** Fuel quantity, altitude, altitude slot values, and wind speed use the grammar rule `positiveNumber` (INT | FLOAT, no minus sign). A value of `0` passes parsing but fuel quantity `0` is rejected at the semantic level. Coordinates and descent rates use `signedNumber` and may be negative.

#### Email Literals

The `pilot` field accepts a full email address token.

```
Pattern: [A-Za-z0-9._%+-]+ '@' [A-Za-z0-9.-]+ '.' [A-Za-z]+
Examples: john.doe@airline.com, pilot@tap.pt
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
| `m` | metres | Altitude, altitude slot values |
| `ft` | feet | Altitude (alternative) |
| `m/s` | metres per second | Wind speed |
| `kts` | knots | Speed (profile entries) |

> **Note:** The `rate` field in a `descend` entry accepts a signed number; its unit is optional (the rate is dimensionless in the current implementation).

#### Separators and Symbols

| Symbol | Usage |
|---|---|
| `{` `}` | Open/close a block |
| `(` `)` | Coordinate groupings |
| `[` `]` | Altitude slot and profile entry groupings |
| `:` | Separates a field name from its value |
| `;` | Terminates a field or entry |
| `,` | Separates values within a coordinate or altitude slot entry |

#### Whitespace and Comments

Whitespace (spaces, tabs, newlines) is freely allowed between tokens and carries no semantic meaning — it exists only to aid readability.

Single-line comments using `//` are supported and ignored by the lexer:

```
// This is a comment
```

---

## 4. Informal Syntactic Structure

### 4.1 Top-Level Structure

A valid DSL file contains **one or more `flight` blocks**. The flight identifier must match the designator format (`xxn(n)(n)(n)(a)` or `xxxn(n)(n)(n)(a)`). All flight identifiers must be unique within the file.

```
flight <FLIGHT-DESIGNATOR> {
    type:      <flight-type>;
    aircraft:  <registration>;
    pilot:     <email>;
    route:     <route-name> from <airport-code> to <airport-code>;

    leg { ... }
    leg { ... }   // one or more legs
}
```

The `route` declaration includes:
- `<route-name>` — an identifier referencing the route in the system
- `from <airport-code>` — the route's origin airport (must match the first leg's departure airport)
- `to <airport-code>` — the route's destination airport (must match the last leg's arrival airport)

The `pilot` field is an email address identifying the pilot responsible for this flight plan. It is validated as a proper email during semantic validation.

### 4.2 Leg Block

Each leg represents one non-stop flight phase between two airports.

```
leg {
    departure { ... }
    arrival   { ... }
    fuel      { ... }
    profile   { ... }   // mandatory

    segment { ... altslot ...; wind ...; mode: climb;   }   // mandatory, must be first
    segment { ... altslot ...; wind ...; mode: cruise;  }   // mandatory, must be second
    segment { ... altslot ...; wind ...; mode: descend; }   // mandatory, must be third
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

Departure date/time must not be in the past. Arrival date/time must be strictly after departure date/time.

### 4.4 Fuel Block

```
fuel {
    quantity: <positive-number> <unit>;
}
```

The quantity must be strictly greater than zero. A value of `0` is syntactically valid but rejected during semantic validation.

### 4.5 Flight Profile Block (Mandatory)

```
profile {
    climb   [ altitude: <number> <unit>, speed: <number> <unit>, rate: <positive-number> <unit>? ];  // 1 or more
    cruise  [ speed: <number> <unit> ];                                                                // 1 or more
    descend [ altitude: <number> <unit>, speed: <number> <unit>, rate: <signed-number> <unit>? ];     // 1 or more
}
```

The profile block is **mandatory** in every leg. All three entry types (`climb`, `cruise`, `descend`) must have at least one entry each. Climb entries must appear in ascending altitude order; descend entries in descending altitude order. The rate of climb is a positive number (the unit is optional); the rate of descent must be negative (the unit is optional).

### 4.6 Segment Blocks

Every leg must contain exactly three `segment` blocks in fixed order. Each segment carries start and end 3D coordinates, one or more altitude slots, a wind block, and a mandatory mode that determines its position. The grammar enforces the order: any deviation is a syntactic error.

```
// First segment — climb from departure elevation to cruise altitude
segment {
    from: (<latitude>, <longitude>, altitude: <number> <unit>);
    to:   (<latitude>, <longitude>, altitude: <number> <unit>);
    altslot [ min: <number> <unit>, max: <number> <unit>, width: <number> <unit> ];
    wind { direction: <degrees>; speed: <number> m/s; }
    mode: climb;
}

// Second segment — cruise at constant altitude
segment {
    from: (<latitude>, <longitude>, altitude: <number> <unit>);
    to:   (<latitude>, <longitude>, altitude: <number> <unit>);
    altslot [ min: <number> <unit>, max: <number> <unit>, width: <number> <unit> ];
    wind { direction: <degrees>; speed: <number> m/s; }
    mode: cruise;
}

// Third segment — descend from cruise altitude to destination elevation
segment {
    from: (<latitude>, <longitude>, altitude: <number> <unit>);
    to:   (<latitude>, <longitude>, altitude: <number> <unit>);
    altslot [ min: <number> <unit>, max: <number> <unit>, width: <number> <unit> ];
    wind { direction: <degrees>; speed: <number> m/s; }
    mode: descend;
}
```

Additional constraints:
- `from` and `to` coordinates must differ.
- Altitude slot fields: `min >= 0`, `max > min`, `width > 0`.
- Wind direction in [0, 359], speed strictly positive and at most 25 m/s.
- Latitude in [-90, 90], longitude in [-180, 180].
- Altitude within physical bounds: [-500 m, 13700 m] or [-1640 ft, 45000 ft].
- Climb segment: `to` altitude must be strictly greater than `from` altitude.
- Cruise segment: `from` and `to` altitudes must be equal.
- Descend segment: `to` altitude must be strictly less than `from` altitude.
- The `to` coordinate of a segment must match the `from` coordinate of the immediately following segment.

### 4.7 Full Structure Diagram

```
flight
├── type
├── aircraft
├── pilot (email)
├── route <name> from <origin> to <destination>
└── leg [1..*]
    ├── departure
    │   ├── airport
    │   ├── date        (not in the past)
    │   └── time
    ├── arrival
    │   ├── airport
    │   ├── date
    │   └── time        (after departure)
    ├── fuel
    │   └── quantity <unit>   (> 0)
    ├── profile [1..1]  (mandatory)
    │   ├── climb [1..*]   → altitude (ascending), speed, rate (rate of climb, positive)
    │   ├── cruise [1..*]  → speed
    │   └── descend [1..*] → altitude (descending), speed, rate (negative)
    ├── segment (climb)   [exactly 1, first]
    │   ├── from  → (lat, lon, altitude)   altitude < to.altitude
    │   ├── to    → (lat, lon, altitude)
    │   ├── altslot [1..*] → min, max, width (all with units)
    │   ├── wind  → direction (0–359), speed (0–25 m/s)
    │   └── mode  = climb
    ├── segment (cruise)  [exactly 1, second]
    │   ├── from / to altitudes must be equal
    │   ├── altslot [1..*], wind
    │   └── mode  = cruise
    └── segment (descend) [exactly 1, third]
        ├── from  → altitude > to.altitude
        ├── altslot [1..*], wind
        └── mode  = descend
```

---

## 5. Block Reference

A condensed reference of every block and its mandatory vs. optional fields.

### `flight` block

| Field | Type | Mandatory | Description |
|---|---|---|---|
| `type` | keyword (`regular`\|`charter`) | Yes | Type of flight |
| `aircraft` | registration (permissive identifier) | Yes | Aircraft registration identifier (e.g. `CS-TNA`, `A320`) |
| `pilot` | email address | Yes | Email of the pilot responsible for this flight plan |
| `route` | identifier + `from` airport + `to` airport | Yes | Route reference with explicit origin and destination airports |
| `leg` | block | Yes (1 or more) | One leg per non-stop journey segment |

### `leg` block

| Field | Type | Mandatory | Description |
|---|---|---|---|
| `departure` | block | Yes | Departure information |
| `arrival` | block | Yes | Arrival information |
| `fuel` | block | Yes | Fuel load for this leg |
| `profile` | block | Yes | Altitude/speed climb, cruise and descent tables |
| `segment` (climb)   | block | Yes | First segment: climb — aircraft gains altitude from departure |
| `segment` (cruise)  | block | Yes | Second segment: cruise — aircraft flies level at cruise altitude |
| `segment` (descend) | block | Yes | Third segment: descend — aircraft loses altitude to destination |

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
| `climb` | entry list | Yes (1+) | One or more climb profile entries in ascending altitude order |
| `cruise` | entry list | Yes (1+) | One or more cruise profile entries |
| `descend` | entry list | Yes (1+) | One or more descend profile entries in descending altitude order |

Each `climb` entry: `altitude` (number + unit), `speed` (number + unit, must be > 0), `rate` (rate of climb, positive number; unit optional).  
Each `cruise` entry: `speed` (number + unit, must be > 0).  
Each `descend` entry: `altitude` (number + unit), `speed` (number + unit, must be > 0), `rate` (must be negative; unit optional).

> **Semantic consistency rule:** the profile must have at least one entry for each of `climb`, `cruise`, and `descend`.

> **Semantic bounds-matching rule:** the minimum and maximum altitudes of `climb` entries must match the climb segment's `from` and `to` altitudes respectively; the maximum and minimum altitudes of `descend` entries must match the descend segment's `from` and `to` altitudes respectively.

### `segment` block (all three types)

All three segment types share the same field structure. The `mode` value is fixed per position and enforced syntactically — it is not a free choice.

| Field | Type | Mandatory | Description |
|---|---|---|---|
| `from` | coordinate tuple | Yes | Start point (lat, lon, altitude); must differ from `to` |
| `to` | coordinate tuple | Yes | End point (lat, lon, altitude); must differ from `from` |
| `altslot` | entry (1 or more) | Yes | Altitude slot: `min`, `max`, `width` (all with units) |
| `wind` | block | Yes | Wind conditions: `direction` (0–359°) and `speed` (m/s, > 0, ≤ 25) |
| `mode` | fixed keyword | Yes | Determined by position: `climb` (1st), `cruise` (2nd), `descend` (3rd) |

> **Altitude direction rules:**
> - Climb: `to.altitude > from.altitude`
> - Cruise: `to.altitude == from.altitude`
> - Descend: `to.altitude < from.altitude`

> **Segment continuity rule:** `segment[i].to` coordinates must equal `segment[i+1].from` coordinates.

### `altslot` entry

| Field | Type | Mandatory | Constraint |
|---|---|---|---|
| `min` | positive number + unit | Yes | `>= 0`; must be less than `max` |
| `max` | positive number + unit | Yes | `>= 0`; must be greater than `min` |
| `width` | positive number + unit | Yes | `> 0` |

### `wind` block

| Field | Type | Mandatory | Constraint |
|---|---|---|---|
| `direction` | positive number (degrees) | Yes | In range [0, 359] |
| `speed` | positive number + `m/s` | Yes | `> 0` and `<= 25` |

---

## 6. Valid Example

This example represents a single-leg regular flight from Porto (OPO) to Madrid (MAD), with a pilot reference, three segments, and a full profile.

```
// Flight plan: OPO → MAD
// Regular flight TP1234, route AA123

flight TP1234 {
    type:      regular;
    aircraft:  A320;
    pilot:     pilot@airline.com;
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
            climb   [ altitude: 69 m,    speed: 210 kts, rate: 10 m/s ];
            climb   [ altitude: 1000 m,  speed: 210 kts, rate: 10 m/s ];
            climb   [ altitude: 2000 m,  speed: 220 kts, rate: 10 m/s ];
            climb   [ altitude: 5000 m,  speed: 260 kts, rate: 10 m/s ];
            climb   [ altitude: 10000 m, speed: 300 kts, rate: 10 m/s ];
            climb   [ altitude: 9249 m,  speed: 300 kts, rate: 10 m/s ];

            cruise  [ speed: 300 kts ];

            descend [ altitude: 9249 m, speed: 300 kts, rate: -10 ];
            descend [ altitude: 5000 m, speed: 290 kts, rate: -10 ];
            descend [ altitude: 2000 m, speed: 250 kts, rate: -7  ];
            descend [ altitude: 1000 m, speed: 180 kts, rate: -7  ];
            descend [ altitude: 610 m,  speed: 140 kts, rate: -5  ];
        }

        // Segment 1: climb from OPO elevation to cruising altitude
        segment {
            from: (41.262891, -8.68522,  altitude: 69 m);
            to:   (42.0,      -8.01,     altitude: 9249 m);
            altslot [ min: 0 m, max: 9249 m, width: 50 m ];
            wind { direction: 270; speed: 15 m/s; }
            mode: climb;
        }

        // Segment 2: cruise at constant altitude
        segment {
            from: (42.0,    -8.01, altitude: 9249 m);
            to:   (41.0,    -4.3,  altitude: 9249 m);
            altslot [ min: 9000 m, max: 9500 m, width: 50 m ];
            wind { direction: 270; speed: 20 m/s; }
            mode: cruise;
        }

        // Segment 3: descend toward MAD
        segment {
            from: (41.0,    -4.3,    altitude: 9249 m);
            to:   (40.4895, -3.5643, altitude: 610 m);
            altslot [ min: 0 m, max: 9249 m, width: 50 m ];
            wind { direction: 260; speed: 10 m/s; }
            mode: descend;
        }

    }
}
```

---

## 7. Invalid Examples and Error Cases

Each example below corresponds to a specific validation rule and serves as a negative test case for the ANTLR grammar and semantic validator.

> **Note on error types:**
> - **Lexical error** — unrecognised character or malformed token (e.g. wrong date format, malformed flight designator).
> - **Syntactic error** — token stream violates grammar rules (e.g. missing mandatory block, unrecognised keyword). The grammar itself catches these before semantic validation.
> - **Semantic error** — syntactically valid but domain-invalid (e.g. duplicate airport, fuel = 0, invalid altitude slot, date in the past).

---

### 7.1 Missing `fuel` block — Syntactic Error

```
// ERROR: leg is missing the mandatory fuel block

flight TP1234 {
    type:      regular;
    aircraft:  A320;
    pilot:     pilot@airline.com;
    route:     AA123 from OPO to MAD;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        // profile and segments omitted for brevity
    }
}
```
**Expected error:** Syntactic — missing mandatory `fuel` block inside `leg`.

---

### 7.2 Missing `profile` block — Syntactic Error

```
// ERROR: leg is missing the mandatory profile block

flight TP1234 {
    type:      regular;
    aircraft:  A320;
    pilot:     pilot@airline.com;
    route:     AA123 from OPO to MAD;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        fuel { quantity: 40200 kg; }
        // No profile block — syntactic error
        segment { ... mode: climb;   }
        segment { ... mode: cruise;  }
        segment { ... mode: descend; }
    }
}
```
**Expected error:** Syntactic — `leg` must contain a `profile` block; the parser rejects its absence.

---

### 7.3 Arrival Airport Mismatch Between Legs — Semantic Error

```
// ERROR: arrival of leg 1 (MAD) does not match departure of leg 2 (LIS)

flight TP5678 {
    type:      regular;
    aircraft:  A320;
    pilot:     pilot@airline.com;
    route:     TP001 from OPO to BCN;

    leg {
        departure { airport: OPO; date: 2026-05-05; time: 08:00; }
        arrival   { airport: MAD; date: 2026-05-05; time: 09:30; }
        // ... profile and segments omitted
    }

    leg {
        departure { airport: LIS; date: 2026-05-05; time: 11:00; }
        arrival   { airport: BCN; date: 2026-05-05; time: 13:00; }
        // ... profile and segments omitted
    }
}
```
**Expected error:** Semantic — arrival airport of leg 1 (`MAD`) must equal departure airport of leg 2 (`LIS`).

---

### 7.4 Negative Fuel Quantity — Syntactic Error

```
// ERROR: positiveNumber does not allow a minus sign

fuel {
    quantity: -500 kg;    // invalid: positiveNumber = INT | FLOAT (no minus)
}
```
**Expected error:** Syntactic — the grammar rule `positiveNumber` does not accept a minus sign; `-500` is an unexpected token.

---

### 7.5 Invalid Flight Type — Syntactic Error

```
// ERROR: "private" is not a valid flight type

flight TP1234 {
    type:  private;    // invalid: grammar only accepts "regular" or "charter"
    ...
}
```
**Expected error:** Syntactic — the parser rule `flightType` only matches `regular` or `charter`; any other word is an unexpected token.

---

### 7.6 Segment Start Equals End — Semantic Error

```
// ERROR: segment start and end coordinates are identical

segment {
    from: (41.26, -8.68, altitude: 9249 m);
    to:   (41.26, -8.68, altitude: 9249 m);    // identical to from
    altslot [ min: 0 m, max: 9249 m, width: 50 m ];
    wind { direction: 270; speed: 15 m/s; }
    mode: climb;
}
```
**Expected error:** Semantic — segment `from` and `to` coordinates must differ.

---

### 7.7 Duplicate Airport in Flight — Semantic Error

```
// ERROR: OPO appears twice (departure of leg 1 and arrival of leg 2)

flight TP9999 {
    type:      regular;
    aircraft:  A320;
    pilot:     pilot@airline.com;
    route:     XX001 from OPO to OPO;
    // ...legs omitted
}
```
**Expected error:** Semantic — the same airport (`OPO`) cannot be visited twice in a flight.

---

### 7.8 Malformed Date — Lexical Error

```
// ERROR: date is not in YYYY-MM-DD format

departure {
    airport: OPO;
    date:    05-05-2026;    // wrong format — must be YYYY-MM-DD
    time:    08:00;
}
```
**Expected error:** Lexical — date token does not match the expected pattern `YYYY-MM-DD`.

---

### 7.9 Duplicate Flight Identifier — Semantic Error

```
// ERROR: TP1234 used as flight identifier in two separate flight blocks

flight TP1234 { ... }

flight TP1234 { ... }   // duplicate identifier
```
**Expected error:** Semantic — flight identifier `TP1234` appears more than once in the same file.

---

### 7.10 Route Origin/Destination Mismatch — Semantic Error

```
// ERROR: route declares origin LIS and destination BCN,
//        but leg departs OPO and arrives MAD

flight TP7777 {
    type:      regular;
    aircraft:  A320;
    pilot:     pilot@airline.com;
    route:     RP001 from LIS to BCN;

    leg {
        departure { airport: OPO; ... }   // OPO ≠ LIS
        arrival   { airport: MAD; ... }   // MAD ≠ BCN
        // ...
    }
}
```
**Expected errors:** Semantic — route origin (`LIS`) must match first leg departure (`OPO`); route destination (`BCN`) must match last leg arrival (`MAD`).

---

### 7.11 Zero Fuel Quantity — Semantic Error

```
// ERROR: 0 passes the parser (valid positiveNumber) but is rejected semantically

fuel { quantity: 0 kg; }
```
**Expected error:** Semantic — fuel quantity must be strictly greater than zero.

---

### 7.12 Missing `wind` Block in Segment — Syntactic Error

```
// ERROR: segment is missing the mandatory wind block

segment {
    from: (41.262891, -8.68522, altitude: 69 m);
    to:   (40.4895, -3.5643, altitude: 610 m);
    altslot [ min: 0 m, max: 9249 m, width: 50 m ];
    mode: climb;
    // wind block is absent — this is a syntactic error
}
```
**Expected error:** Syntactic — each `segment` must contain a `wind` block; the parser rejects its absence.

---

### 7.13 Missing `altslot` Entry in Segment — Syntactic Error

```
// ERROR: segment is missing the mandatory altslot entry

segment {
    from: (41.262891, -8.68522, altitude: 69 m);
    to:   (40.4895, -3.5643, altitude: 610 m);
    wind { direction: 270; speed: 15 m/s; }
    mode: climb;
    // altslot entry is absent — this is a syntactic error
}
```
**Expected error:** Syntactic — each `segment` must contain at least one `altslot` entry; the parser rejects its absence.

---

### 7.14 Departure Date in the Past — Semantic Error

```
// ERROR: departure date is before the current date

departure {
    airport: OPO;
    date:    2024-01-01;    // in the past
    time:    08:00;
}
```
**Expected error:** Semantic — departure date/time must not be in the past.

---

### 7.15 Arrival Before Departure — Semantic Error

```
// ERROR: arrival time is before departure time

departure { airport: OPO; date: 2026-05-05; time: 09:00; }
arrival   { airport: MAD; date: 2026-05-05; time: 08:00; }   // earlier than departure
```
**Expected error:** Semantic — arrival date/time must be strictly after departure date/time.

---

### 7.16 Segment Discontinuity — Semantic Error

```
// ERROR: end of climb segment does not match start of cruise segment

segment {
    from: (41.26, -8.68, altitude: 69 m);
    to:   (42.0,  -8.01, altitude: 9249 m);   // ends here
    ...
    mode: climb;
}

segment {
    from: (99.0, 0.0, altitude: 9249 m);       // different lat/lon — discontinuous
    to:   (41.0, -4.3, altitude: 9249 m);
    ...
    mode: cruise;
}
```
**Expected error:** Semantic — segment 2 start coordinates must match segment 1 end coordinates.

---

### 7.17 Climb Altitude Direction Violated — Semantic Error

```
// ERROR: climb segment ends at a lower altitude than it starts

segment {
    from: (41.26, -8.68, altitude: 9249 m);
    to:   (42.0,  -8.01, altitude: 69 m);    // lower than from — invalid for climb
    ...
    mode: climb;
}
```
**Expected error:** Semantic — climb segment must have strictly increasing altitude.

---

### 7.18 Wind Speed Exceeds Maximum — Semantic Error

```
// ERROR: wind speed is above the operational maximum

wind { direction: 270; speed: 30 m/s; }   // exceeds 25 m/s limit
```
**Expected error:** Semantic — wind speed exceeds the maximum allowed value of 25 m/s.

---

### 7.19 Invalid Pilot Email — Semantic Error

```
// ERROR: pilot field does not contain a valid email address

flight TP1234 {
    type:      regular;
    aircraft:  A320;
    pilot:     notAnEmail;    // no @ sign
    route:     AA123 from OPO to MAD;
    ...
}
```
**Expected error:** Semantic — `pilot` must be a valid email address.

---

### 7.20 Profile Missing Cruise Entries — Semantic Error

```
// ERROR: profile has no cruise entries but leg has a cruise segment

profile {
    climb   [ altitude: 69 m,   speed: 210 kts ];
    climb   [ altitude: 9249 m, speed: 300 kts ];
    descend [ altitude: 9249 m, speed: 300 kts, rate: -10 ];
    descend [ altitude: 610 m,  speed: 140 kts, rate: -5  ];
    // no cruise entries
}
```
**Expected error:** Semantic — leg has a cruise segment but no cruise profile entries.

---

### 7.21 Profile Climb Bounds Mismatch — Semantic Error

```
// ERROR: profile climb min altitude does not match climb segment from altitude

// Climb segment goes from 69 m to 9249 m
segment { from: (..., altitude: 69 m); to: (..., altitude: 9249 m); mode: climb; ... }

profile {
    climb [ altitude: 500 m,  speed: 210 kts ];   // min is 500, not 69 — mismatch
    climb [ altitude: 9249 m, speed: 300 kts ];
    ...
}
```
**Expected error:** Semantic — climb profile minimum altitude must match climb segment departure altitude.

---

## 8. Design Decisions and Notes

### 8.1 Why block-based syntax?

A hierarchical, block-delimited syntax (`{ }`) was chosen because:
- It is self-evidently structured and readable by humans.
- It maps cleanly onto ANTLR parser rules (each block becomes a rule).
- It scales naturally to multi-leg flights without ambiguity.
- It is consistent with widely adopted configuration DSLs (HCL, JSON, etc.).

### 8.2 Why is `profile` mandatory?

The `profile` block is a team extension that provides climb, cruise, and descent speed/altitude tables for the simulation engine. It is mandatory because every flight plan imported into AISafe must be ready for simulation: without a profile, the physics engine has no speed or rate data to work with. The semantic validator also enforces that all three entry types (`climb`, `cruise`, `descend`) are present — a profile with only climb and descend entries cannot drive the cruise phase of the simulation.

### 8.3 Why allow multiple `flight` blocks in one file?

A single DSL file may describe a batch of flight plans (e.g. a day's schedule). The grammar allows `flightPlan+` (one or more). The uniqueness constraint on flight identifiers ensures each flight is unambiguously identifiable within the file.

### 8.4 Why is the flight designator a dedicated lexer rule and not a general identifier?

The flight designator follows a precise industry-standard format used worldwide for airline operations. Encoding it as a dedicated `FLIGHT_DESIGNATOR` lexer rule — rather than accepting any identifier and checking format semantically — means:

1. **Immediate rejection** — a file with a malformed flight designator is rejected at the lexer before the parser even runs.
2. **Unambiguous tokenisation** — the lexer can distinguish a flight designator (`TP1234`, `TAP123`) from a plain identifier without lookahead.
3. **Self-documenting grammar** — the grammar rule expresses the format constraint precisely.

The designator pattern was extended from 2 to 2–3 uppercase letters to support ICAO airline codes (e.g. `TAP`, `AFR`, `IBE`) in addition to IATA codes (e.g. `TP`, `AF`, `IB`). This keeps the DSL compatible with both naming conventions used in the aviation industry.

### 8.5 Why are segments required in fixed order (climb → cruise → descend)?

Every real-world fixed-wing flight follows the same invariant sequence. Encoding this as a syntactic constraint (three separate parser rules — `climbSegment`, `cruiseSegment`, `descendSegment` — in fixed order inside `leg`) rather than a semantic one has two advantages:

1. **Earlier rejection** — a file with a missing or reordered segment is rejected at the parser stage, producing a clearer error.
2. **Simpler validator** — the semantic validator can assume the three segments are always present and in order.

### 8.6 Why is a `pilot` field required?

Each flight plan must be traceable to a responsible pilot. The pilot is identified by their email address — an external identifier that links the DSL file to the user account in the AISafe system. Using an email (rather than a numeric ID) keeps the DSL file self-documenting and independent of internal database identifiers. The semantic validator checks that the value is a syntactically valid email address; the application layer checks that the email corresponds to a registered pilot in the system.

### 8.7 Why enforce altitude direction per segment type?

The direction of altitude change in each phase of flight is a physical invariant, not an optional constraint. A "climb" segment where the aircraft actually descends is not a climb — it is a data entry error. Catching this at the semantic layer provides a clear, targeted error message and prevents the simulation engine from receiving contradictory data (e.g. a climb profile paired with a descending path).

### 8.8 Why enforce segment continuity?

Adjacent segments must share an endpoint: the aircraft must be at the same physical location at the end of one segment as at the start of the next. A discontinuity would mean the aircraft teleports between segments, which is physically impossible. Validating this semantically keeps the grammar simpler while still guaranteeing that the resulting AST represents a coherent flight path.

### 8.9 Why enforce profile bounds matching?

The profile altitude table must span exactly the altitude range of the corresponding segment. If the climb profile starts at 500 m but the climb segment departs from 69 m, the simulation has no speed data for the initial phase of the climb. Enforcing that `profMin == seg.from.altitude` and `profMax == seg.to.altitude` ensures the simulation can always interpolate a speed from the profile table for any point along the segment.

### 8.10 Why cap wind speed at 25 m/s?

25 m/s (~49 kts) is a commonly cited operational wind limit for commercial aircraft ground operations and a conservative ceiling for en-route wind data in this simulation context. Values above this are almost certainly input errors rather than realistic flight conditions. Rejecting them semantically prevents the physics engine from producing unrealistic results.

### 8.11 Relationship to the ANTLR grammar

| DSL concept | ANTLR artefact |
|---|---|
| Keywords (e.g. `flight`, `leg`) | Lexer rules with character-class alternation for case-insensitivity |
| Flight designator (`TP1234`, `TAP123`) | `FLIGHT_DESIGNATOR` lexer rule: `[A-Z][A-Z][A-Z]?[0-9][0-9]?[0-9]?[0-9]?[A-Z]?` |
| Aircraft registration | `aircraftId` parser rule: accepts `(ID \| INT \| MINUS \| IATA_CODE \| ICAO_CODE \| FLIGHT_DESIGNATOR)+` — permissive to handle hyphenated registrations |
| Pilot reference | `pilotRef` parser rule: accepts `EMAIL \| ID` |
| Route reference name | `routeRef` parser rule: accepts `ID \| FLIGHT_DESIGNATOR` (e.g. `myRoute` or `AA123`) |
| Airport codes | `IATA_CODE` / `ICAO_CODE` lexer rules |
| Numeric literals | `INT` / `FLOAT` lexer rules |
| Date / Time literals | `DATE_LITERAL` / `TIME_LITERAL` lexer rules |
| Email literal | `EMAIL` lexer rule |
| Block structure | Parser rules (`flightPlan`, `leg`, `departure`, etc.) |
| Segment order/structure | Three separate parser rules (`climbSegment`, `cruiseSegment`, `descendSegment`) in fixed order |
| Altitude slot | `altitudeSlot` parser rule: `altslot [ min: … , max: … , width: … ]` |
| Wind block | `wind` parser rule: `wind { direction: …; speed: … m/s; }` |
| Descent rate unit | Optional `unit?` after the signed rate number |
| Semantic constraints | `FlightSemanticValidator` traversing the AST built by `FlightAstBuilderVisitor` |

### 8.12 Error reporting

All three validation stages must produce clear, actionable errors:

- **Lexical errors** — unrecognised character or malformed token. Must report line and column.
- **Syntactic errors** — missing or unexpected token. Must report line, column, and what was expected.
- **Semantic errors** — structurally valid but domain-invalid content. Must report which rule was violated and where.

All semantic errors are **collected and reported together** in a single execution pass — the validator does not abort on the first error.
