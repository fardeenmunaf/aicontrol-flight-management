# AlSafe – Domain Model Aggregate Justification

> **Project:** AISafe – Air Traffic Management System  
> **Artefact:** US011 – Aggregate Justification Document  
> **Version:** 2.0  

---

## Preamble – DDD Concepts Applied

This document justifies the aggregate design decisions of the AlSafe domain model using the vocabulary of **Domain-Driven Design (DDD)**. The following key concepts are referenced throughout:

| DDD Term | Meaning in this context |
|---|---|
| **Aggregate** | A cluster of domain objects (Entities and Value Objects) treated as a single unit of consistency, with one designated root. |
| **Aggregate Root** | The only entry point into the aggregate. External objects may only hold references to the root, never to internal members directly. |
| **Entity** | An object with a distinct, stable identity that persists across state changes. Two entities with the same attributes are not the same if their identities differ. |
| **Value Object (VO)** | An immutable object defined entirely by its attributes. It has no conceptual identity. Two VOs with the same values are interchangeable. |
| **Transactional Boundary** | The scope within which all invariants must be consistent. Changes within an aggregate are atomic; cross-aggregate changes rely on eventual consistency. |
| **Invariant** | A business rule that must always hold true within the aggregate boundary, regardless of the state transition that occurs. |
| **Reference-by-Identity** | The DDD pattern where cross-aggregate associations are maintained via Value Object IDs (e.g., `AircraftId`), never via direct object references. |
| **Bounded Context** | A semantic boundary within which a model and its ubiquitous language are consistently defined and applied. |
| **Ubiquitous Language** | The shared vocabulary between domain experts and developers, embedded in the code and documentation. |
| **Domain Event** | A record of something significant that happened in the domain (e.g., `FlightPlanValidated`, `SafetyViolationDetected`). |
| **Repository** | An abstraction that provides collection-like access to aggregates, hiding persistence details. One repository per aggregate root. |
| **Factory** | A domain construct responsible for creating complex aggregate instances, ensuring invariants are satisfied from the moment of creation. |
| **CQRS** | Command Query Responsibility Segregation — commands mutate state; queries read state. Influences how `SimulationReport` is modelled. |


---

## Modelling Conventions & Diagram Simplifications

The domain model diagram is the **primary communication artefact** for the team and stakeholders. To keep it legible and avoid visual clutter, a set of deliberate simplifications was applied. These are **not modelling errors** — they are conscious trade-offs between strict DDD fidelity and diagram readability, following the principle that *a domain model is a communication tool, not an implementation specification*.

### Cross-Aggregate ID References Not Shown as Explicit VOs

In a fully rigorous DDD implementation, every cross-aggregate reference identifier (e.g., `NodeId` used inside `Segment`, `AirportId` used inside `FlightLeg`) would be declared as a dedicated Value Object **class** within the referencing aggregate. However, doing so would require duplicating VO boxes (e.g., `NodeId`, `AirportId`) inside the `FlightPlan` aggregate package, creating visual noise and making aggregate boundaries harder to read.

> **Convention adopted:** Cross-aggregate ID references that are already declared as Value Objects in their *home* aggregate (e.g., `NodeId` in `Node`, `AirportId` in `Airport`) are referenced **by name** inside the using aggregate (e.g., inside `FlightLeg` and `Segment`) without redeclaring the VO box. This is understood to mean: *the field holds an instance of the corresponding VO type defined in the home aggregate*.

### Repeated Value Object Types Across Aggregates

Certain Value Object *types* (same structure, same semantics) appear in multiple aggregates without being shared via inheritance or a common package. Examples:

| VO Type | Appears in |
|---|---|
| `Coordinates` (latitude, longitude) | `Airport` aggregate, `Node` aggregate |
| `Maker` (name, country) — named as `AircraftMaker` and `EngineMaker` in their respective aggregates | `AircraftModel` aggregate, `EngineModel` aggregate |

In a full implementation, these would be *shared kernel* types defined in a common module and reused across bounded contexts. In the diagram, they are shown as separate VO boxes within each aggregate **to avoid cross-package arrows**, which would imply a coupling that does not exist at the aggregate boundary level. Each aggregate owns its own copy of the VO definition with context-specific naming (e.g., `AircraftMaker` for aircraft manufacturers, `EngineMaker` for engine manufacturers).

### Summary

| Simplification | Strict DDD Alternative | Reason for Simplification |
|---|---|---|
| `NodeId` not re-declared inside `FlightPlan` | Declare `NodeId <<VO>>` inside `FlightPlan` package | Avoids duplicated VO boxes; ID type is unambiguous from context |
| `AirportId` not re-declared inside `FlightPlan` | Declare `AirportId <<VO>>` inside `FlightPlan` package | Same rationale as above |
| `PilotId`, `AircraftId` not re-declared inside `FlightPlan` | Declare each as `<<VO>>` inside `FlightPlan` | Cross-aggregate references are self-evident from relationship labels |
| `Coordinates` duplicated in `Airport` and `Node` | Extract to shared kernel module | Prevents cross-aggregate VO arrows that imply structural coupling |
| `AircraftMaker` and `EngineMaker` duplicated | Extract to shared `Maker` kernel type | Enables context-aware naming while maintaining type coherence across aggregates |

---

## Table of Contents

1. [User Aggregate](#1-user-aggregate)
2. [Pilot Aggregate](#2-pilot-aggregate)
3. [AirTransportCompany Aggregate](#3-airtransportcompany-aggregate)
4. [AirControlArea Aggregate](#4-aircontrolarea-aggregate)
5. [Airport Aggregate](#5-airport-aggregate)
6. [Node Aggregate](#6-node-aggregate)
7. [AircraftModel Aggregate](#7-aircraftmodel-aggregate)
8. [EngineModel Aggregate](#8-enginemodel-aggregate)
9. [Aircraft Aggregate](#9-aircraft-aggregate)
10. [FlightRoute Aggregate](#10-flightroute-aggregate)
11. [Flight Aggregate](#11-flight-aggregate)
12. [FlightPlan Aggregate](#12-flightplan-aggregate)
13. [Simulation Aggregate](#13-simulation-aggregate)
14. [WeatherData Aggregate](#14-weatherdata-aggregate)
15. [SimulationReport Aggregate](#15-simulationreport-aggregate)

---

## 1. User Aggregate

### Aggregate Root: `User`

**Bounded Context:** Identity & Access Management (IAM)

**Justification:**

The `User` aggregate encapsulates the *Identity and Access Management (IAM)* subdomain of the AISafe system. `User` is designated as the Aggregate Root because all access-control invariants — security clearance expiry, skills assessment scheduling, and activation status — are enforced at the user level. No external context may directly mutate these properties without going through the root, thereby guaranteeing transactional consistency in authentication and authorisation workflows.


The `User` aggregate has a *long-lived lifecycle*. It does not get deleted — disabling is the terminal operational state, preserving audit traceability. This is a domain invariant: a deactivated user must remain identifiable in the system for historical referencing (e.g., past flight plans that referenced their `UserId`).

**Internal Structure:**

- **`UserId`** *(Value Object)*: An immutable, globally unique identifier generated upon user creation. As a VO, two `UserId` instances carrying the same value are semantically identical and fully interchangeable. It acts as the cross-aggregate reference token, propagated to `Pilot`, `AirTransportCompany`, and other aggregates without creating inter-aggregate coupling.
- **`Role`** *(Enumeration)*: Modelled as an enumeration representing predefined system roles (e.g., `PILOT`, `AIR_TRAFFIC_CONTROLLER`, `FLIGHT_DISPATCHER`, `MAINTENANCE_ENGINEER`, `AIRPORT_MANAGER`, `SYSTEM_ADMINISTRATOR`). The enumeration contains a fixed, closed set of role values, each with a `designation` field. Using an enumeration instead of an Entity enforces type safety and prevents arbitrary role values from entering the domain.

**Key Invariants Enforced:**

- `activeSecurityClearanceExpiry` must always be a future or present date for an `ACTIVE` user.
- `skillsAssessmentDueDate` must comply with the 5-year regulatory assessment cycle.
- A user cannot become `ACTIVE` without a valid email from an approved domain.
- Role assignments are contained within this aggregate, preventing orphan `Role` records.

---

## 2. Pilot Aggregate

### Aggregate Root: `Pilot`

**Bounded Context:** Aviation Operations – Crew Management

**Justification:**

`Pilot` is deliberately separated from the `User` aggregate to honour the *Single Responsibility Principle* at the domain level and respect the Bounded Context boundary between IAM and Aviation Operations. Although every pilot must also be a system user, the pilot's operational attributes — type-rating certifications, roster status, and flight assignments — are aviation-domain concerns governed by their own transactional boundary. Merging these into the `User` aggregate would violate the principle of *cohesive invariant ownership* and cause the IAM aggregate to take on operational aviation semantics it should not know about.


A `Pilot` aggregate instance cannot be deactivated while it holds active `FlightPlan` assignments. This lifecycle constraint is enforced through a domain service or application-layer pre-condition check, since the `FlightPlan` aggregate is outside this boundary — an example of a *cross-aggregate lifecycle dependency* resolved through eventual consistency or application-level guard.

**Internal Structure:**

- **`PilotId`** *(Value Object)*: A stable, immutable identifier for the pilot's *operational identity*, distinct from `UserId` which represents their *system identity*. This dual-identity design supports the principle of *context isolation* — the pilot's operational record can evolve (e.g., certification changes) independently of their system account credentials.

**Key Invariants Enforced:**

- A `Pilot` must hold at least one valid type-rating (`AircraftModelId`) to be assigned to a `FlightPlan`.
- A `Pilot` in `INACTIVE` status cannot be referenced in new `FlightPlan` instances.
- The linkage to the `User` aggregate via `UserId` is a *reference-by-identity* — the pilot aggregate does not own or replicate user data.

---

## 3. AirTransportCompany Aggregate

### Aggregate Root: `AirTransportCompany`

**Bounded Context:** Aviation Operations – Airline Management

**Justification:**

An Air Transport Company is a first-class domain concept acting as the organisational owner of a fleet (`Aircraft`), a portfolio of flight routes (`FlightRoute`), and a roster of collaborators (`User`) and pilots (`Pilot`). The aggregate boundary is defined by the invariant that IATA and ICAO codes must be globally unique across all registered companies. As the root of this aggregate, `AirTransportCompany` governs resource ownership consistency without directly embedding the data of its associated aggregates — it holds only their identity references.


The `AirTransportCompany` lifecycle is deliberately *non-terminal* in the current domain scope. It is treated as *reference data* with a stable identity, consistent with its role as an organisational anchor for other aggregates.

**Internal Structure:**

The aggregate contains no nested Entities or Value Objects beyond its scalar attributes. Associations to collaborators, routes, and aircraft are maintained as collections of cross-aggregate IDs (`UserId`, `FlightRouteId`, `AircraftId`), following strict *reference-by-identity* to avoid aggregate boundary violations.

**Key Invariants Enforced:**

- `iataCode` (2 letters) and `icaoCode` (2–3 letters) must each be globally unique.
- A `UserId` can only be added to this aggregate if the referenced `User` aggregate exists and is `ACTIVE`.
- An `AircraftId` can only be held here once the corresponding `Aircraft` aggregate has been successfully instantiated.

---

## 4. AirControlArea Aggregate

### Aggregate Root: `AirControlArea`

**Bounded Context:** Airspace Management

**Justification:**

An `AirControlArea` represents a *bounded airspace jurisdiction* — a geographically defined region managed by a flight control authority. It is modelled as an independent aggregate because its lifecycle is completely independent to flights, simulations, or weather records: it is registered by Backoffice Operators and persists as long-lived, stable reference data. The aggregate enforces uniqueness of `areaCode` and validity of `geographicBoundaries` within its own transactional boundary.


**Internal Structure:**

- **`AirControlAreaId`** *(Value Object)*: Immutable identifier propagated as a cross-aggregate reference token to `Airport`, `Simulation`, `WeatherData`, and `FlightLeg` (indirectly via destination airport) aggregates.

**Key Invariants Enforced:**

- `areaCode` must be unique across all registered areas — enforced at creation time by the domain (uniqueness invariant).
- `geographicBoundaries` must describe a valid, non-degenerate closed polygon.
- `minimumRequiredArrivalFuel` must be a strictly positive numeric value, encoding the regulatory fuel reserve requirement for the area's destination airports.

---

## 5. Airport Aggregate

### Aggregate Root: `Airport`

**Bounded Context:** Airspace Management – Infrastructure

**Justification:**

Airports are stable, geographically anchored domain objects with globally unique identifiers (IATA and ICAO codes). They serve as *departure and arrival anchors* for `FlightLeg` entities and as *endpoints* for `FlightRoute` aggregates. Their aggregate boundary isolates the invariants of location validity and code uniqueness, preventing invalid geographic data from propagating into simulation and planning aggregates.


Airports are *long-lived reference data*. Their stable identity (`AirportId`) is referenced extensively across aggregates; premature deletion would violate referential integrity in historical flight records.

**Internal Structure:**

- **`AirportId`** *(Value Object)*: Immutable identifier referenced by `FlightLeg` entities inside `FlightPlan`.
- **`Coordinates`** *(Value Object)*: Encapsulates `latitude` and `longitude` as an *atomic, immutable* pair. Modelled as a VO because geographic coordinates carry no independent identity — what matters is the value pair. Replacing coordinates with an equal-valued VO produces no observable domain difference.

> **Note on shared VO types:** `Coordinates` uses the same structure as the one in the `Node` aggregate. Rather than introducing a cross-aggregate shared kernel dependency in the diagram, each aggregate declares its own copy of the VO. In implementation, both would map to the same `Coordinates` value type.

**Key Invariants Enforced:**

- `iataCode` and `icaoCode` must each be globally unique within the system.
- `Coordinates` must represent a valid geographic position (latitude ∈ [−90°, 90°], longitude ∈ [−180°, 180°]).
- `elevation` must be non-negative (metres above sea level).
- An airport must be associated with exactly one `AirControlArea` via `AirControlAreaId`.

---

## 6. Node Aggregate

### Aggregate Root: `Node`

**Bounded Context:** Airspace Management – Navigation Network

**Justification:**

A `Node` is a named geographic waypoint in the aeronautical route network — the point at which two or more `Segment` path elements connect. It is modelled as a standalone aggregate because nodes are *shared infrastructure*: a single node may be referenced by many distinct `Segment` entities belonging to different `FlightPlan` aggregates. This shared nature mandates an independent lifecycle, a stable identity, and a dedicated Repository.



**Internal Structure:**

- **`NodeId`** *(Value Object)*: Stable cross-aggregate identifier referenced by `Segment` entities within the `FlightPlan` aggregate to define routing waypoints. **Diagram Simplification Note — `NodeId`:**

`NodeId` is declared as a Value Object within the `Node` aggregate's home package. When referenced inside `Segment` (within `FlightPlan`), it is not redeclared as a VO box inside that package — this is the *cross-aggregate ID reference* convention described in the Modelling Conventions section above. Conceptually, `Segment.startNodeId` and `Segment.endNodeId` are typed as `NodeId` Value Objects.
- **`Coordinates`** *(Value Object)*: Geographic position of the node, semantically identical to `AirportCoordinates` — an immutable, identity-less pair of lat/lon values.

**Key Invariants Enforced:**

- `designation` must be unique and descriptive within the navigation network.
- `Coordinates` must represent a valid position.
- A `Node` cannot be decommissioned while active `Segment` references point to its `NodeId`.

---

## 7. AircraftModel Aggregate

### Aggregate Root: `AircraftModel`

**Bounded Context:** Aircraft Catalogue

**Justification:**

`AircraftModel` represents a *type-level specification* — a catalogue entry defining the aerodynamic, structural, and performance characteristics shared by all physical aircraft of that model. **This is a classic DDD *type vs. instance* separation: the model (type) and the aircraft (instance) have separate lifecycles and separate invariants.** Changes to the model specification should not cascade uncontrollably into individual `Aircraft` instances or `FlightPlan` computations; the aggregate boundary enforces this **isolation**.



The lifecycle constraint at termination illustrates a *cross-aggregate lifecycle dependency*: the `AircraftModel` aggregate's termination is blocked by the existence of `Aircraft` aggregate instances referencing its `AircraftModelId` — a business rule enforced at the application layer via a domain service.

**Internal Structure:**

- **`AircraftModelId`** *(Value Object)*: Identifier referenced by `Aircraft`, `Pilot`, and `AircraftModel` itself (self-referential, for variant tracking).
- **`AircraftMaker`** *(Value Object)*: Encapsulates `name` and `country` of the manufacturer. Modelled as a VO because manufacturer information is *descriptive metadata* — it has no independent domain lifecycle. Two `AircraftModel` instances from the same manufacturer share equivalent `AircraftMaker` VOs.

> **Note on shared VO types:** `AircraftMaker` uses the same structure as the one in the `EngineModel` aggregate (named `EngineMaker`). As with `Coordinates`, each aggregate owns its own VO declaration in the diagram to avoid cross-aggregate structural coupling arrows. In implementation, both would inherit from or reference a common `Maker` base type.

**Key Invariants Enforced:**

- The combination of model name and `AircraftMaker` must be globally unique (composite key invariant).
- At least one certified `EngineModel` (`EngineModelId`) must always be associated — the last engine cannot be removed.
- All aerodynamic parameters (`dragCoefficient`, `liftCoefficient`, `wingArea`, `wingSpan`) must be strictly positive, as they feed directly into the lift/drag physics equations in the simulation.
- `maximumTakeOffWeight` ≥ `maximumZeroFuelWeight` ≥ `emptyWeight` is a structural physics invariant.

---

## 8. EngineModel Aggregate

### Aggregate Root: `EngineModel`

**Bounded Context:** Aircraft Catalogue – Propulsion

**Justification:**

Engine models constitute a distinct *propulsion catalogue* subdomain with an independent lifecycle. An `EngineModel` encapsulates the thrust and fuel-consumption parameters (`thrustSeaLevelStopped`, `thrustSeaLevelCruise`, `thrustSpecificFuelConsumption`) that directly feed the physics simulation. Separating this into its own aggregate allows engine specifications to be created, updated, versioned, or retired without impacting the `AircraftModel` aggregate's consistency boundary — a classic application of *aggregate autonomy*.


**Internal Structure:**

- **`EngineModelId`** *(Value Object)*: Cross-aggregate identifier propagated to `AircraftModel` for the certified-engine association.
- **`EngineMaker`** *(Value Object)*: Manufacturer descriptor — same VO semantics as `AircraftMaker` in `AircraftModel`. No independent lifecycle within this context.
- **`FuelProperties`** *(Value Object)*: Encapsulates `type`, `density`, and `specificEnergy` as an *atomic, immutable* physical specification. It is a VO because fuel properties are a scientific constant characterising the propellant — they have no independent identity, and two `FuelProperties` instances with identical values are interchangeable domain objects.

**Key Invariants Enforced:**

- The combination of `name` and `EngineMaker` must be globally unique.
- `thrustSpecificFuelConsumption` must be strictly positive.
- `FuelProperties.density` and `specificEnergy` must be physically valid values.
- An `EngineModel` cannot be disassociated from all `AircraftModel` references if actual `Aircraft` instances actively use that model combination.

---

## 9. Aircraft Aggregate

### Aggregate Root: `Aircraft`

**Bounded Context:** Aviation Operations – Fleet Management

**Justification:**

`Aircraft` represents the *operational instance* of an `AircraftModel` — a specific, registered physical airframe owned by an `AirTransportCompany`. The aggregate boundary governs the lifecycle of a physical aircraft from registration through to decommissioning. It enforces the global uniqueness of the registration number, the validity of its cabin configuration relative to model capacity, and the operational status transitions — all of which are invariants that must hold within a single transactional boundary.



A `RETIRED` aircraft represents a *terminal lifecycle state*: no new `FlightPlan` can reference a retired `Aircraft`. However, historical `FlightPlan` records that reference its `AircraftId` must remain intact for auditability — the aggregate is not deleted, only transitioned to a terminal state. This models the *soft-delete* or *deactivation* lifecycle pattern.

**Internal Structure:**

- **`AircraftId`** *(Value Object)*: Globally unique, stable identifier referenced by `FlightPlan`, `AircraftPosition`, `SafetyViolation`, and `AirTransportCompany`.
- **`CabinConfiguration`** *(Value Object)*: Encapsulates the seat count per class (`firstClassSeats`, `businessClassSeats`, `economyClassSeats`) as an immutable value object. It is a VO because the configuration describes *a physical property of the aircraft*, not an independently identifiable domain concept. Replacing the entire configuration with a new one of different counts is a valid mutation — the identity of the aircraft does not change.

**Key Invariants Enforced:**

- `registrationNumber` must be globally unique.
- Total seats in `CabinConfiguration` must not exceed the associated `AircraftModel`'s declared capacity.
- A `RETIRED` aircraft cannot be referenced in new `FlightPlan` instances.
- `operationalStatus` follows a defined, non-reversible state machine: `OPERATIONAL` → `RETIRED`.

---

## 10. FlightRoute Aggregate

### Aggregate Root: `FlightRoute`

**Bounded Context:** Aviation Operations – Route Planning

**Justification:**

A `FlightRoute` is a *scheduled commercial air service template* between two airports — a reusable operational structure from which individual `Flight` instances are generated. Its independent aggregate boundary reflects the fact that routes have their own lifecycle (`activeFrom`, `activeUntil`) and can be deactivated without retroactively affecting already-scheduled flights. This temporal lifecycle management is a key invariant that justifies the aggregate boundary.



The deactivation lifecycle transition creates a *temporal invariant*: a route cannot be deactivated if flights scheduled after the proposed `activeUntil` date already reference it. This is a cross-aggregate lifecycle constraint enforced via an application-layer domain service.

**Internal Structure:**

- **`FlightRouteId`** *(Value Object)*: Stable identifier referenced by `AirTransportCompany` and `Flight`.

**Key Invariants Enforced:**

- `designation` must follow the format: two company initials + up to four digits (e.g., `TP123`), globally unique.
- `activeUntil` cannot be set if `Flight` instances with dates after it already exist.
- A `Flight` can only be instantiated under an `ACTIVE` route.

---

## 11. Flight Aggregate

### Aggregate Root: `Flight`

**Bounded Context:** Aviation Operations – Flight Scheduling

**Justification:**

`Flight` represents a *specific, scheduled occurrence* of a `FlightRoute` — a concrete departure event with a unique flight designator and scheduled times. It is intentionally separated from `FlightPlan` (the operational execution document) to model the conceptual distinction between *scheduling* (what is planned at the route level) and *execution* (how the specific flight will be carried out). This separation also enables a flight to exist (scheduled) without yet having an associated plan, which is a valid domain state.



**Internal Structure:**

- **`FlightId`** *(Value Object)*: Cross-aggregate identifier referenced from `FlightRoute` and `Simulation`.

**Key Invariants Enforced:**

- `flightDesignator` must conform to the IATA format (`xxn(n)(n)(n)(a)`) and be globally unique.
- `scheduledArrivalTime` must be strictly after `scheduledDepartureTime` — a temporal ordering invariant.
- `type` must be one of the defined enumerated values (`REGULAR`, `CHARTER`).

---

## 12. FlightPlan Aggregate

### Aggregate Root: `FlightPlan`

**Bounded Context:** Aviation Operations – Flight Execution Planning

**Diagram Simplification Note — `FlightPlan` Aggregate:**

The `FlightPlan` aggregate relies on several cross-aggregate identifiers (`AircraftId`, `PilotId`, `AirportId`, `NodeId`) that are declared as Value Objects in their respective home aggregates. To maintain diagram readability, these VO types are **not redeclared** as separate boxes within the `FlightPlan` package. In a production implementation, these would be fully-typed VO fields (e.g., `FlightLeg.departureAirportId: AirportId`, `Segment.startNodeId: NodeId`). The absence of the VO box in this aggregate's package is a *diagram convention*, not an omission of the VO pattern.

**Justification:**

`FlightPlan` is the most structurally complex aggregate in the domain. It is the *execution blueprint* for a specific flight occurrence, coordinating the assignment of an `Aircraft`, a `Pilot`, and a hierarchical routing specification composed of `FlightLeg`, `Segment`, and `AltitudeSlot` entities. The aggregate boundary is justified by the strong *transactional consistency* requirements: fuel quantities, leg-sequence coherence, altitude slot validity, and resource assignments must all be validated atomically. This is the aggregate that the DSL (Domain Specific Language) ultimately populates.


The state machine for `FlightPlan.status` is a critical lifecycle invariant owned exclusively by the Aggregate Root. No external aggregate may directly transition this status — it must go through the root.

**Internal Structure:**

- **`FlightPlanId`** *(Value Object)*: Immutable identifier referenced by `Flight`.
- **`FlightLeg`** *(Entity)*: A single non-stop journey between two airports (`sequenceNumber`, `departureTime`, `arrivalTime`, `fuelQuantity`). It is an Entity because it has a domain identity (`sequenceNumber`) within the plan and mutable operational state. Its lifecycle is *wholly owned* by the `FlightPlan` root — `FlightLeg` has no meaning outside the context of its parent plan.
- **`Segment`** *(Entity)*: A linear path between two `Node` waypoints, carrying environmental context (`windDirection`, `windSpeed`). It is an Entity because segments have a positional identity within a leg and carry mutable data that may be updated (e.g., weather-enriched). Modelled with a *composition* relationship to `FlightLeg` — it cannot exist independently.
- **`AltitudeSlot`** *(Entity)*: An authorised altitude band within a segment (`minimumAltitude`, `maximumAltitude`, `width`). It is an Entity because each slot has an individually identifiable constraint that feeds directly into flight simulation path validation. Its lifecycle is scoped to the `Segment` that contains it, and transitively to the `FlightPlan` aggregate.

**Key Invariants Enforced:**

- At least one `FlightLeg` must be present.
- Arrival airport of `FlightLeg[n]` must equal departure airport of `FlightLeg[n+1]` — *leg-sequence coherence* invariant.
- `fuelQuantity` per leg must be strictly positive and satisfy the `minimumRequiredArrivalFuel` of the destination `AirControlArea`.
- `passengerLoad` must not exceed total seat capacity of the assigned `Aircraft`.
- The assigned `Pilot` must hold a valid type-rating for the `AircraftModel` of the assigned `Aircraft`.
- `status` transitions are strictly controlled by the Aggregate Root.

---

## 13. Simulation Aggregate

### Aggregate Root: `Simulation`

**Bounded Context:** Air Traffic Control – Simulation Engine

**Justification:**

`Simulation` is a *bounded execution context* for air traffic control simulation over a defined time window and geographic area. It coordinates real-time aircraft position tracking, safety violation detection, and simulation outcome collection. The aggregate boundary is justified by the need for strong consistency among its child entities: a `SafetyViolation` is only interpretable in the context of the `AircraftPosition` records that triggered it, and all share the same simulation lifecycle. This aggregate is the central artefact for fulfilling the system's scalable air control simulation requirements.



The `status` field of `Simulation` represents the *current phase in its lifecycle state machine*. Only the `Simulation` Aggregate Root may transition this status, enforcing the lifecycle invariant.

**Internal Structure:**

- **`SimulationId`** *(Value Object)*: Identifier referenced by `SimulationReport` to associate a report with its originating simulation.
- **`AircraftPosition`** *(Entity)*: The tracked position of an aircraft at a specific simulation timestep. It is an Entity because each position record has a distinct identity (aircraft + timestamp composite) and forms part of an *append-only event log* used for trajectory reconstruction and collision detection. Once created, `AircraftPosition` records are immutable — they represent a factual recording of what happened during the simulation.
- **`VelocityVector`** *(Value Object)*: Encapsulates `speed` and `direction` as an *immutable, identity-less* descriptor of instantaneous movement. It is a VO because two vectors with the same speed and heading are semantically equivalent — there is no meaningful distinction between two such instances. It appears both within `AircraftPosition` (current motion state) and `SafetyViolation` (motion state at the moment of violation detection).
- **`SafetyViolation`** *(Entity)*: A detected safety event (e.g., two aircraft in dangerously close proximity), recorded with `timestamp` and `severity`. It is an Entity because each violation is individually identifiable and reportable. It holds references to at least two `AircraftId` values (the involved aircraft) and records multiple `VelocityVector` instances to document the collision geometry at the moment of detection.

**Key Invariants Enforced:**

- At least one `FlightId` must be referenced for a simulation to be valid.
- `endDateTime` must be strictly after `startDateTime`.
- `safetyThresholds` must be well-defined, positive values.
- A `SafetyViolation` must reference at least two `AircraftId` values — a violation is definitionally a multi-aircraft event.
- `AircraftPosition` entries are *immutable* once appended — they form an auditable trace.

---

## 14. WeatherData Aggregate

### Aggregate Root: `WeatherData`

**Bounded Context:** Meteorological Services

**Justification:**

`WeatherData` encapsulates meteorological observations or forecasts for a specific `AirControlArea`, recorded at a precise `dateTime` and enriched with the weather parameters `windDirection`, `windSpeed`, `temperature`, and `pressure`. Its separate aggregate boundary is justified by the independence of its acquisition lifecycle: weather data may be registered manually or uploaded remotely through the weather client, and each record is handled as an isolated meteorological snapshot. This acquisition pipeline is decoupled from flight planning and simulation. Moreover, `WeatherData` records are *immutable once registered* — they represent factual meteorological data, not a mutable operational object — which makes them suitable for their own transactional boundary.



`WeatherData` follows an *immutable record* lifecycle pattern once in `REGISTERED` state — no mutations are permitted. This is consistent with the domain concept of meteorological records as historical, factual data points associated with a specific air control area and timestamp.

**Internal Structure:**

- **`WeatherDataId`** *(Value Object)*: Stable identifier referenced by the `Simulation` aggregate to incorporate environmental conditions into the simulation run.

**Key Invariants Enforced:**

- `dateTime` must represent a valid calendar date and time.
- `windSpeed`, `temperature`, and `pressure` must be valid numeric values.
- `windDirection` must be a valid bearing (0°–360°).
- A `WeatherData` record must be scoped to an existing `AirControlArea` (validated via `AirControlAreaId` or the corresponding aggregate root reference, depending on the application layer).
- Records are immutable post-creation — any correction requires a new `WeatherData` instance.

---

## 15. SimulationReport Aggregate

### Aggregate Root: `SimulationReport`

**Bounded Context:** Air Traffic Control – Reporting

**Justification:**

`SimulationReport` is intentionally decoupled from the `Simulation` aggregate on the basis of *separate lifecycle* — A report is generated *after* the simulation concludes, may be regenerated, versioned, or audited independently, and is consumed by Flight Control Operators for regulatory review. This aggregate reflects the *CQRS (Command Query Responsibility Segregation)* pattern: it is a *read-side projection* of the simulation's outcomes, denormalised and optimised for consumption rather than mutation.



The `SimulationReport` lifecycle is *independent* from the `Simulation` lifecycle: a simulation may be archived or purged while its report persists for regulatory audit purposes. This independent persistence requirement is the definitive justification for a separate aggregate boundary.

**Internal Structure:**

The aggregate contains no nested Entities or Value Objects. Its data (`totalFlights`, `validationResult`, `generatedAt`) represents a *denormalised, read-optimised summary* of simulation outcomes. This is a deliberate design choice aligned with CQRS: the report does not replicate the simulation's full object graph but instead materialises a computed, stable projection of it.

**Key Invariants Enforced:**

- A `SimulationReport` must reference exactly one `SimulationId` — it is always bound to a specific simulation.
- `generatedAt` must be chronologically after the `endDateTime` of the referenced `Simulation`.
- `validationResult` must be one of the defined enumerated outcomes (e.g., `PASS`, `FAIL`, `INCONCLUSIVE`).
- The report is *immutable once generated* — any subsequent analysis or correction requires a new report instance, preserving the audit trail.

---

## Cross-Aggregate Linking: Aggregate Roots as the Primary Association Mechanism

### Concept & Rationale

The AlSafe domain model employs a deliberate architectural principle regarding inter-aggregate relationships: **while identifiers (VOs such as `AircraftId`, `FlightId`) are often used for persistence, queries, and references, the primary semantic bond between aggregates is established through direct references to Aggregate Roots, not to their identities.**

This principle serves two critical purposes:

1. **Semantic Clarity:** The presence of an Aggregate Root reference (e.g., a `Flight` object) in a relationship makes explicit the domain concept being linked. It is semantically more expressive to say "`FlightPlan` is *for* a `Flight`" than to say "`FlightPlan` holds a `FlightId`."

2. **Lifecycle Consistency:** Aggregate Root references enable runtime verification of lifecycle constraints and cross-aggregate invariants. For example, a `FlightPlan` should only be created if its referenced `Flight` aggregate actually exists and is in a valid state — a verification best performed by holding the root itself or a reference to it, not merely its ID.

### Example: `FlightPlan` and `Flight`

**Scenario:** A `FlightPlan` is the operational execution blueprint for a specific `Flight`. In the domain language, we express this as: *"A FlightPlan is created **for** a specific Flight."*

**Cross-Aggregate Linkage Pattern:**

- **Semantic Root Reference:** In domain logic, operations involving a `FlightPlan` should accept or reference the `Flight` Aggregate Root (not just its `FlightId`). For example:
  ```
  FlightPlan plan = new FlightPlan(
      flight,          // Reference to the Flight Aggregate Root
      aircraft,        // Reference to the Aircraft Aggregate Root
      pilot,           // Reference to the Pilot Aggregate Root
      flightLegs,
      status
  );
  ```

- **Persistence & Queries:** When persisting or querying, the implementation may store only the `flightId` (foreign key) in the database for normalized storage. However, when *loading* the `FlightPlan` aggregate from the repository, the system should reconstruct or fetch the associated `Flight` aggregate and inject it into the `FlightPlan` instance. This preserves the semantic link while respecting database constraints.

- **Invariant Enforcement:** At the constructor or mutation point, the presence of the `Flight` root allows validation logic to run at the aggregate boundary. For instance:
  ```
  public FlightPlan(Flight flight, Aircraft aircraft, Pilot pilot, ...) {
      // Invariant: Flight must be in SCHEDULED or PLANNING state
      if (!flight.canHavePlan()) {
          throw new InvalidFlightPlanStateException(...);
      }
      // Invariant: Pilot must be certified for the aircraft model
      if (!pilot.isCertifiedFor(aircraft.aircraftModel())) {
          throw new InsufficientCertificationException(...);
      }
      this.flight = flight;  // Store root reference
      this.aircraft = aircraft;
      // ...
  }
  ```

### Contrast: ID-Only Linking

In an ID-only pattern, the `FlightPlan` would reference only `Flight` via its `FlightId`:

```java
public class FlightPlan {
    private FlightId flightId;  // Only the ID, not the root
    
    public FlightPlan(FlightId flightId, ...) {
        // No invariant checking is possible without the Flight root.
        // Caller must ensure Flight validity externally.
        this.flightId = flightId;
    }
}
```

This approach:
- Loses semantic specificity in the code.
- Defers invariant validation to external application services or database constraints, weakening encapsulation.
- Makes it harder to reason about the aggregate's state in isolation.

### Implementation Guidance

**Architecture Layer (Services, Repositories):**

When external clients (e.g., application services) create or mutate a `FlightPlan`, they should:
1. Fetch the referenced `Flight`, `Aircraft`, and `Pilot` aggregates (via their repositories).
2. Pass these root references to the `FlightPlan` constructor or mutation method.
3. Let the `FlightPlan` aggregate enforce its own invariants in terms of these roots.

**Persistence Layer:**

The Object-Relational Mapping (ORM) framework may store only the identity references (foreign keys) in the database. However, the repository implementation should eagerly load or lazy-load associated aggregates to maintain the semantic link in memory.

**Example Repository Implementation:**
```java
public class FlightPlanRepository {
    public FlightPlan findById(FlightPlanId id) {
        FlightPlan plan = orm.findById(id);
        Flight flight = flightRepository.findById(plan.getFlightId());
        Aircraft aircraft = aircraftRepository.findById(plan.getAircraftId());
        Pilot pilot = pilotRepository.findById(plan.getPilotId());
        
        // Reconstruct the aggregate with root references
        plan.setFlight(flight);
        plan.setAircraft(aircraft);
        plan.setPilot(pilot);
        return plan;
    }
}
```

### Benefits in the AlSafe Domain

By linking aggregates via their roots:

1. **Domain-Driven Architecture:** Code directly reflects domain language: *"I have a FlightPlan for this Flight"* rather than *"I have a FlightPlan that references this FlightId".*

2. **Fail-Fast Validation:** Invariants are checked at creation time, preventing invalid states from propagating into the domain.

3. **Temporal Consistency:** If a `Flight` is deleted or transitions to an invalid state, dependent aggregates can be notified or constrained at runtime, not merely through database cascades.

4. **Testability:** Unit tests can verify invariants by injecting mock or test-double aggregate roots, without needing database setup.

---

