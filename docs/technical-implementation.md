# AISafe — Technical Implementation Document

## 1. Project Overview

AISafe is an air traffic safety management system built for multiple operator roles: backoffice operators, airline (air transport) operators, and air control operators. The system manages aircraft, flight plans, routes, simulations, weather data, and safety validations.

The project is structured as a multi-module Maven project composed of ten modules, each with a clearly bounded responsibility. The architecture follows Domain-Driven Design (DDD), Clean Architecture, and enforces SOLID principles throughout.

---

## 2. Module Architecture

### 2.1 Module Breakdown

| Module | Role |
|---|---|
| `aisafe.core` | Domain model and application layer (controllers, repositories interfaces) |
| `aisafe.infrastructure.application` | Cross-cutting: authorization roles, password policy |
| `aisafe.persistence.impl` | Repository implementations (JPA and In-Memory) + factory |
| `aisafe.flight.dsl` | ANTLR-based DSL for flight plan parsing and semantic validation |
| `aisafe.app.backoffice.console` | UI entry point for backoffice operators |
| `aisafe.app.airtransport.console` | UI entry point for airline operators |
| `aisafe.app.aircontrol.console` | UI entry point for air control operators |
| `aisafe.app.bootstrap` | Database seeding and initial data loading |
| `aisafe.app.weather.client` | External weather API integration |
| `aisafe-simulation` | Flight simulation engine |

### 2.2 Dependency Direction

```
aisafe.app.*.console
        │
        ▼
  aisafe.core  ◄──────────────────────────────────────────
        │                                                  │
        ▼                                                  │
aisafe.infrastructure.application        aisafe.persistence.impl
                                                    │
                                                    ▼
                                        eapli.framework / Jakarta JPA
```

The key architectural constraint is that `aisafe.core` defines repository *interfaces* and `aisafe.persistence.impl` provides the *implementations*. Core never depends on persistence — only the reverse. This is the Dependency Inversion Principle applied at module level.

---

## 3. Domain Model

### 3.1 Aggregates

The domain is organized around 15 aggregates, each an `AggregateRoot` implementing the eapli.framework contract. Each aggregate owns its invariants and is the consistency boundary for its data.

**Aircraft** — represents a registered aircraft instance. Stores its registration number, country, operational status, cabin configuration, and references to its model, engine model, and owning company. Enforces that the assigned engine is certified for the aircraft model at construction time.

**AircraftModel** — the technical specification of an aircraft type: weights (empty, MTOW, MZFW), fuel capacity, performance data (cruise speed, service ceiling, aerodynamic coefficients), and the set of certified engine models. Business rules prevent the last certified engine model from being removed.

**EngineModel** — engine type specification. Groups engine metadata with a `FuelProperties` value object (fuel type, density, specific energy).

**FlightPlan** — a plan for a specific flight, composed of legs. Implements a state machine: `DRAFT → TESTED → VOID`. Any structural modification resets a `TESTED` plan back to `DRAFT`. Enforces leg connectivity (the arrival airport of leg N must equal the departure airport of leg N+1).

**FlightLeg** — a single leg within a flight plan, itself composed of segments and altitude slots. Owned entirely by its parent `FlightPlan` (cascade).

**Flight** — a scheduled flight instance with designator, type, and scheduled times. Enforces that arrival is after departure.

**FlightRoute** — a route designation with temporal validity. Implements soft-delete via `deactivateFrom(LocalDate)`.

**Airport** — stores IATA (3 uppercase letters) and ICAO (4 uppercase letters) codes, elevation, coordinates, and a reference to its containing air control area.

**AirControlArea** — geographic boundary with point-in-polygon containment logic and regulatory minimum arrival fuel constraint.

**AirTransportCompany** — airline entity with name, IATA, and ICAO codes.

**Node** — a navigation waypoint with designation and `Coordinates` value object (validated latitude/longitude).

**Pilot** — links a user identity with certification data and certified aircraft models.

**User** — system identity with email (regex-validated), phone number (pattern-validated), security clearance expiry, and role assignments. Provides explicit `enable()`/`disable()` transitions.

**Simulation** — tracks an active or completed simulation with sets of tracked flights, captured weather, aircraft positions, and safety violations. A `SafetyViolation` requires at minimum two velocity vectors and two aircraft involved.

**SimulationReport** — links to a simulation and captures aggregated validation results.

**CustomerCollaborator** — represents an external operator tied to either an air transport company or an air control area (never both). Type-based invariant: `AIR_TRANSPORT_COMPANY` type requires a company ID and forbids a control area ID, and vice versa.

### 3.2 Value Objects

All aggregate identities are value objects (UUID-based, `@Embeddable`). The domain also uses rich embedded value objects to avoid primitive obsession:

| Value Object | Fields | Validation |
|---|---|---|
| `Coordinates` | latitude, longitude | latitude ∈ [−90, 90], longitude ∈ [−180, 180] |
| `CabinConfiguration` | economy, business, first seats | non-negative counts |
| `FuelProperties` | FuelType, density, specificEnergy | non-null type, positive numerics |
| `AircraftModelMaker` / `EngineModelMaker` | name, country | non-blank strings |
| `RouteDesignation` | designation string | pattern `^[A-Z]{2}[0-9]{1,4}$` |
| `AirportCoordinates` | latitude, longitude | — |

### 3.3 Cross-Aggregate References

Aggregates reference each other exclusively by identity (ID value objects), following DDD guidelines. For example, `FlightPlan` stores `aircraftId` as a string; `FlightLeg` stores `departureAirportId` and `arrivalAirportId` as `AirportId` value objects. Full object references are only used within the same aggregate boundary (e.g., `Aircraft → AircraftModel`).

---

## 4. Layered Architecture

The project implements a four-layer clean architecture:

### 4.1 Presentation Layer (`aisafe.app.*.console`)

Console-based entry points for each operator role. These classes wire together controllers and repository factories and delegate all logic to the application layer. They contain no business rules.

### 4.2 Application Layer (Controllers in `aisafe.core`)

Application controllers (e.g., `RegisterAircraftController`, `ImportFlightPlanController`) orchestrate use cases. Their responsibilities are:

- Enforcing authorization (role checks via `AuthzRegistry`)
- Coordinating multiple repositories
- Enforcing cross-aggregate invariants that span transactional boundaries
- Delegating domain-specific logic to aggregates

Controllers receive repository interfaces as constructor arguments (dependency injection) and never instantiate concrete implementations directly.

### 4.3 Domain Layer (`aisafe.core`)

Pure domain objects: aggregates, entities, value objects, enumerations, and repository interfaces. No framework dependencies beyond eapli.framework domain contracts. This layer defines the ubiquitous language of the system.

### 4.4 Infrastructure Layer (`aisafe.persistence.impl`)

Concrete repository implementations and the factory that creates them. The infrastructure layer depends on the domain layer (not the reverse). It provides two full sets of implementations: JPA-backed for production, and in-memory for development and testing.

---

## 5. SOLID Principles

### 5.1 Single Responsibility Principle (SRP)

Each class has one reason to change.

**Aircraft vs. AircraftModel:** `Aircraft` manages instance state (registration, operational status, company assignment). `AircraftModel` manages technical specifications and engine certifications. Neither class handles persistence, authorization, or UI.

**Repository segregation:** `JpaAircraftRepository` translates domain queries into JPA queries. `InMemoryAircraftRepository` manages an in-memory collection. The `AircraftRepository` interface defines only the domain contract. Each class changes for a single reason.

**DSL pipeline stages:** `FlightDSLLexer` tokenizes. `FlightDSLParser` builds syntax trees. `FlightAstBuilderVisitor` constructs the AST. `FlightSemanticValidator` checks semantics. Each stage is independently replaceable and testable.

**Controllers vs. aggregates:** `RegisterAircraftController` handles authorization and orchestration. `Aircraft` handles domain invariants. Validation of cross-aggregate weight constraints (e.g., passenger load vs. MZFW) lives in the controller, not in the aggregate, because it requires data from multiple aggregates.

### 5.2 Open/Closed Principle (OCP)

Modules are open for extension and closed for modification.

**Persistence backend:** The `RepositoryFactory` interface is a stable contract. Adding a new persistence backend (e.g., MongoDB) means implementing the interface without modifying any controller or domain class. The `JpaRepositoryFactory` and `InMemoryRepositoryFactory` demonstrate this with two fully independent implementations.

**Repository extensions:** Domain-specific query methods are added to individual repository interfaces (e.g., `AircraftRepository.findByRegistrationNumber()`) without modifying the base `DomainRepository` contract.

**DSL semantic validation:** New semantic rules are added by extending the validator's rule set without modifying the AST builder or parser.

### 5.3 Liskov Substitution Principle (LSP)

Subtypes are substitutable for their base types.

**Repository implementations:** `JpaAircraftRepository` and `InMemoryAircraftRepository` both implement `AircraftRepository`. Any controller coded against the interface works correctly regardless of which implementation is injected. The in-memory implementation does not weaken any postcondition or strengthen any precondition relative to the interface.

**Role hierarchy in authorization:** The `AISafeRoles` constants define system roles. Controllers check only for required roles. A user with a higher-privileged role passes lower-privilege checks without violating any behavioral contract.

### 5.4 Interface Segregation Principle (ISP)

Clients depend only on interfaces they use.

**Repository interfaces are domain-scoped:** `AircraftRepository` exposes only aircraft-related queries. `AirportRepository` exposes only airport-related queries. No controller is forced to depend on a fat repository interface with unrelated methods.

**Controller dependencies are minimal:** `RegisterAircraftController` declares dependencies on `AircraftRepository`, `AircraftModelRepository`, `EngineModelRepository`, and `CustomerCollaboratorRepository` — only what it actually needs. It does not depend on flight plan or simulation repositories.

**DSL components:** `FlightDslProcessor` depends only on the lexer, parser, visitor, and validator interfaces — not on persistence or authentication.

### 5.5 Dependency Inversion Principle (DIP)

High-level modules depend on abstractions, not concretions.

**Core ↔ Persistence:** `aisafe.core` defines `RepositoryFactory` and all repository interfaces. `aisafe.persistence.impl` implements them. The dependency flows from infrastructure to domain, not the reverse. Swapping persistence implementations requires zero changes to any controller or domain class.

**Constructor injection:** Controllers receive repository interfaces via constructor. The concrete type is resolved externally (in the application entry point), ensuring that no high-level module creates its own low-level dependencies.

```
// High-level controller depends on interface
public RegisterAircraftController(
    AircraftRepository aircraftRepository,
    AircraftModelRepository aircraftModelRepository,
    ...) { ... }

// Concrete type provided externally
RepositoryFactory factory = new JpaRepositoryFactory();
new RegisterAircraftController(factory.aircraft(), factory.aircraftModels(), ...);
```

---

## 6. Design and Implementation Patterns

### 6.1 Repository Pattern

**What it is:** An abstraction layer between the domain model and data access logic. Repository interfaces are defined in the domain layer; implementations live in the infrastructure layer.

**Where it appears:** Every aggregate has a corresponding repository interface in `aisafe.core` under `*/repositories/`. Twelve repository interfaces are defined, each extending `DomainRepository<Id, Aggregate>` from eapli.framework, with additional domain-specific query methods.

**Dual implementation:** Each repository interface has both a JPA implementation (`JpaXxxRepository`) and an in-memory implementation (`InMemoryXxxRepository`). This makes the application fully testable without a database.

**Example — AircraftRepository:**
```java
// Interface (domain layer)
public interface AircraftRepository extends DomainRepository<AircraftId, Aircraft> {
    Optional<Aircraft> findByRegistrationNumber(String registrationNumber);
    Iterable<Aircraft> findByOperationStatus(String status);
    Iterable<Aircraft> findByAirTransportCompanyId(AirTransportCompanyId id);
    boolean existsByAircraftModel(AircraftModelId modelId);
}
```

### 6.2 Abstract Factory Pattern

**What it is:** A factory that produces families of related objects without specifying their concrete classes.

**Where it appears:** `RepositoryFactory` is the abstract factory interface. It declares factory methods for all 12 repositories, each in two variants: one for standalone use and one accepting a `TransactionalContext` for auto-transaction management.

**Two concrete factories:**
- `JpaRepositoryFactory` — creates JPA-backed repository instances
- `InMemoryRepositoryFactory` — creates in-memory repository instances (returns `null` for `newTransactionalContext()` since in-memory needs no transactions)

The factory is the single point where infrastructure types are selected. Switching from JPA to in-memory requires changing one line in the application entry point.

```java
// Abstract factory
public interface RepositoryFactory {
    AircraftRepository aircraft();
    AircraftRepository aircraft(TransactionalContext autoTx);
    AircraftModelRepository aircraftModels();
    // ... 10 more
    TransactionalContext newTransactionalContext();
}

// JPA concrete factory
public class JpaRepositoryFactory implements RepositoryFactory {
    @Override
    public AircraftRepository aircraft() {
        return new JpaAircraftRepository(PersistenceSettings.getPersistenceUnitName());
    }
}

// In-memory concrete factory
public class InMemoryRepositoryFactory implements RepositoryFactory {
    @Override
    public AircraftRepository aircraft() {
        return new InMemoryAircraftRepository();
    }
}
```

### 6.3 Template Method Pattern

**What it is:** A base class defines the skeleton of an algorithm; subclasses fill in specific steps without changing the overall structure.

**Where it appears:** `BaseJpaRepositoryBase<T, K, I>` extends eapli.framework's `JpaAutoTxRepository`. It provides the wiring for both the `PersistenceSettings` constructor path and the `TransactionalContext` constructor path. Concrete JPA repositories extend this base and only implement domain-specific query methods — the save, find-by-id, delete, and transaction lifecycle are inherited.

This pattern eliminates boilerplate JPA code from every concrete repository while keeping domain-specific logic explicit.

### 6.4 Builder Pattern (within DSL pipeline)

**What it is:** Separates the construction of a complex object from its representation, allowing the same process to produce different results.

**Where it appears:** The DSL processing pipeline in `aisafe.flight.dsl` constructs a `FlightDslDocument` (and ultimately a `FlightDslProcessingResult`) through a sequence of transformation steps:

```
Input (file/string)
  → FlightDSLLexer          (tokenization)
  → FlightDSLParser         (syntax tree)
  → FlightStructureListener (structure tracking)
  → FlightAstBuilderVisitor (AST construction)
  → FlightDslDocument       (intermediate representation)
  → FlightSemanticValidator (validation)
  → FlightDslProcessingResult (final product with errors)
```

`FlightDslProcessor` orchestrates this pipeline. Each intermediate product (`FlightPlanAst`, `LegAst`, `SegmentAst`, `DepartureAst`, `ArrivalAst`, `FuelAst`, etc.) is an AST node built incrementally by the visitor.

### 6.5 Visitor Pattern

**What it is:** A way to separate an algorithm from the object structure it operates on. New operations can be added without modifying the objects.

**Where it appears:** `FlightAstBuilderVisitor` implements the ANTLR visitor interface and traverses the parse tree produced by `FlightDSLParser`. For each grammar rule node, the visitor extracts semantic data and constructs the corresponding AST object. This cleanly separates AST construction from the grammar definition and from the semantic validation that follows.

### 6.6 Strategy Pattern (DSL Validation)

**What it is:** Defines a family of algorithms, encapsulates each, and makes them interchangeable.

**Where it appears:** `FlightSemanticValidator` provides pluggable semantic validation rules applied to the `FlightDslDocument` after parsing. Each rule is an independent check (e.g., leg connectivity, coordinate bounds, wind direction range, altitude slot ordering, existence of referenced aircraft and airports). The rules produce typed `SemanticError` instances with a `SemanticErrorType` enum identifying the failure category. New rules can be added without modifying the validator's interface.

### 6.7 Listener Pattern (Parse Tree Walking)

**What it is:** An event-driven traversal where a listener receives callbacks as the tree walk enters and exits nodes.

**Where it appears:** `FlightStructureListener` implements the ANTLR listener interface and walks the parse tree to produce a `FlightStructureSummary` with `SourceLocation` metadata for each significant element. This is used for diagnostic reporting (line numbers, positions of errors) independently of the AST construction step.

### 6.8 Facade Pattern

**What it is:** Provides a simplified interface to a complex subsystem.

**Where it appears:** Application controllers act as facades. `RegisterAircraftController`, for example, coordinates four repositories and multiple validation steps behind a single `registerAircraft(...)` method. The UI layer calls one method and receives a result; it is shielded from the complexity of authorization checks, cross-aggregate validation, and transaction management.

Similarly, `ImportFlightPlanController` hides the full DSL pipeline (parsing, AST building, semantic validation, domain entity creation, persistence) behind a single import method.

### 6.9 Value Object Pattern

**What it is:** An immutable object defined by its attributes rather than its identity. Two value objects with the same attributes are considered equal.

**Where it appears:** Pervasively throughout the domain layer. Every aggregate identity is a value object (UUID-wrapped). Domain primitives that carry validation logic are expressed as value objects:

- `Coordinates` enforces geographic bounds at construction time
- `RouteDesignation` enforces the `^[A-Z]{2}[0-9]{1,4}$` pattern
- `FuelProperties` groups fuel type, density, and specific energy into one coherent unit
- `CabinConfiguration` groups seat counts by cabin class

This eliminates primitive obsession and pushes validation to the point of construction.

### 6.10 Aggregate Pattern (DDD)

**What it is:** A cluster of domain objects treated as a unit for data changes. One object (the aggregate root) controls access to the cluster and enforces its invariants.

**Where it appears:** All fifteen domain aggregates implement `AggregateRoot` from eapli.framework. The aggregate root is the only entry point for modifications. Sub-entities (e.g., `FlightLeg` within `FlightPlan`, `SafetyViolation` within `Simulation`) are accessed and modified only through the root.

Cascade is configured at the JPA level to reflect this: `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` on collections owned by the aggregate root ensures the database state stays consistent with the domain model.

### 6.11 State Machine Pattern

**What it is:** Models an object whose behavior changes based on its internal state.

**Where it appears:** `FlightPlan` implements an explicit state machine:

```
DRAFT  ──markAsTested()──►  TESTED  ──voidTest()──►  VOID
  ▲                            │
  └────addLeg() / removeLeg()──┘  (any structural change resets to DRAFT)
```

Invalid transitions throw `IllegalStateException`. The state field is a `FlightPlanStatus` enum, and every mutating method checks the current state before proceeding.

`Aircraft` also implements a state machine for operational status (`IN_SERVICE → MAINTENANCE → DECOMISSIONED`), with guards preventing invalid transitions such as decommissioning an already-decommissioned aircraft.

---

## 7. Domain-Specific Language (DSL)

### 7.1 Purpose

The flight plan DSL allows flight plan specifications to be written in a structured text format and imported into the system. It provides both syntactic and semantic validation before any domain objects are created.

### 7.2 Technology

The DSL is implemented with ANTLR (Another Tool for Language Recognition). The grammar is compiled into `FlightDSLLexer` and `FlightDSLParser`. The project then applies the Visitor and Listener patterns on top of the generated code.

### 7.3 Processing Pipeline

**Stage 1 — Lexing:** `FlightDSLLexer` converts the raw input into a token stream.

**Stage 2 — Parsing:** `FlightDSLParser` applies the grammar rules and produces a parse tree.

**Stage 3 — Structure tracking:** `FlightStructureListener` walks the parse tree and records source locations for diagnostic messages.

**Stage 4 — AST construction:** `FlightAstBuilderVisitor` traverses the parse tree and produces a `FlightDslDocument` — an intermediate representation with typed AST nodes (`FlightPlanAst`, `LegAst`, `SegmentAst`, `DepartureAst`, `ArrivalAst`, `FuelAst`, `ProfileAst`, `CoordinateAst`).

**Stage 5 — Semantic validation:** `FlightSemanticValidator` applies domain rules to the `FlightDslDocument` and returns a list of `SemanticError` objects. Validated constraints include: leg connectivity, coordinate bounds, wind direction range `[0, 360)`, altitude slot ordering, and existence of referenced aircraft and airports.

**Stage 6 — Result assembly:** `FlightDslProcessor` wraps the document and any validation errors into a `FlightDslProcessingResult` returned to the caller.

### 7.4 Integration

`ImportFlightPlanController` receives a DSL input (file path or string), runs it through `FlightDslProcessor`, checks for errors, and if clean, maps the `FlightDslDocument` to domain entities (`FlightPlan`, `FlightLeg`, `Segment`, `AltitudeSlot`) which are persisted via the flight plan repository.

---

## 8. Authorization and Security

### 8.1 Role Definitions

Roles are defined as constants in `AISafeRoles` (in `aisafe.infrastructure.application`):

| Role Constant | Role Code | Associated User Type |
|---|---|---|
| `ADMIN` | `ADMIN` | System administrator |
| `BACKOFFICE_OPERATOR` | `BACKOFFICE_OPERATOR` | Backoffice staff |
| `AIR_TRANSPORT_COMPANY_COLLABORATOR` | `ATCC` | Airline operators |
| `PILOT` | `PILOT` | Pilots |
| `FLIGHT_CONTROL_OPERATOR` | `FCO` | Air control operators |
| `WEATHER_PERSON` | `WEATHER_PERSON` | Weather data providers |

### 8.2 Enforcement

Authorization is enforced at the start of every controller method via the eapli.framework authorization service:

```java
authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
```

This call throws an `UnauthorizedException` if the current user does not hold the required role. The check occurs before any repository access or domain modification.

### 8.3 Password Policy

`AISafePasswordPolicy` extends `AbstractPasswordPolicy` from eapli.framework with project-specific password validation rules. This is wired into the authentication infrastructure in `AuthzConfigurator`.

---

## 9. Persistence Strategy

### 9.1 JPA Repository Implementation

JPA repositories extend `BaseJpaRepositoryBase`, which itself extends eapli.framework's `JpaAutoTxRepository`. This provides:

- Automatic transaction management when a `TransactionalContext` is supplied
- Inherited CRUD operations (`save`, `ofIdentity`, `findAll`, `delete`)
- JPQL query helpers (`match`, `matchOne`)

Each concrete JPA repository implements only its domain-specific query methods.

### 9.2 In-Memory Repository Implementation

In-memory repositories maintain a `HashMap<Id, Aggregate>` and implement all query methods with Java stream operations. They share the same interface as JPA repositories and are used for bootstrapping tests and development without a database.

### 9.3 Transactional Context

When a controller needs to coordinate multiple repository writes in a single transaction, it acquires a `TransactionalContext` from the factory and passes it to each repository constructor. The context wraps the JPA `EntityTransaction` and ensures atomicity.

```java
TransactionalContext tx = factory.newTransactionalContext();
AircraftRepository aircraftRepo = factory.aircraft(tx);
// ...
tx.commit();
```

---

## 10. Validation Strategy

Validation is applied at three levels, each with a distinct responsibility:

**Level 1 — Value Object construction:** Domain primitives validate at construction time. `Coordinates` enforces latitude/longitude bounds. `RouteDesignation` enforces the naming pattern. This makes invalid value objects impossible to instantiate.

**Level 2 — Aggregate invariants:** Aggregate constructors and mutating methods enforce business rules that involve only the aggregate's own data. `FlightPlan.addLeg()` checks leg connectivity. `Aircraft` constructor checks engine certification. `AircraftModel.removeEngineModel()` prevents removal of the last certified engine.

**Level 3 — Application layer:** Cross-aggregate constraints that require data from multiple aggregates are enforced in controllers. Weight-payload constraints (requiring both aircraft model data and cabin configuration) and type-based constraints for `CustomerCollaborator` (requiring knowledge of which companies and areas exist) are examples.

---

## 11. Summary of Patterns Applied

| Pattern | Category | Location |
|---|---|---|
| Repository | Architectural | `aisafe.core/*/repositories/`, `aisafe.persistence.impl` |
| Abstract Factory | Creational | `RepositoryFactory`, `JpaRepositoryFactory`, `InMemoryRepositoryFactory` |
| Template Method | Behavioral | `BaseJpaRepositoryBase` |
| Builder | Creational | `FlightDslProcessor` pipeline |
| Visitor | Behavioral | `FlightAstBuilderVisitor` |
| Strategy | Behavioral | `FlightSemanticValidator` |
| Listener | Behavioral | `FlightStructureListener` |
| Facade | Structural | Application controllers |
| Value Object | Domain | All `*Id` types and embedded value objects |
| Aggregate | Domain | All `AggregateRoot` implementations |
| State Machine | Domain | `FlightPlan`, `Aircraft` |

---

## 12. GRASP Patterns

GRASP (General Responsibility Assignment Software Patterns) provides nine principles for deciding which class should hold which responsibility. The following documents where each applicable principle is realised in this codebase.

### 12.1 Information Expert

**Principle:** Assign responsibility to the class that has the information needed to fulfil it.

**Where it appears:**

- Value objects validate themselves at construction time: `Coordinates` enforces geographic bounds, `RouteDesignation` enforces the naming pattern (see section 6.9 for the full list). Each is the expert on its own validity.
- `AircraftModel` decides whether an engine model can be removed (`removeEngineModel()`) because it owns the set of certified engines and knows the rule that the last one may not be removed.
- `FlightPlan` checks leg connectivity on `addLeg()` — it holds all current legs and is the only class that can verify the arrival-departure continuity invariant.
- `AirControlArea` performs point-in-polygon containment checks — it holds the boundary polygon and is the natural expert on whether a coordinate falls within it.

### 12.2 Creator

**Principle:** Assign class B the responsibility to create instances of class A if B contains, aggregates, records, or closely uses A.

**Where it appears:**

- `FlightPlan` creates `FlightLeg` instances via `addLeg()` — it aggregates and owns legs (cascade all, orphan removal).
- `FlightLeg` creates `Segment` and `AltitudeSlot` instances — it owns these sub-objects within the aggregate boundary.
- `RepositoryFactory` creates all repository instances — it aggregates all repository types and is the designated creator for both JPA and in-memory variants.
- `FlightDslProcessor` creates `FlightDslDocument` and `FlightDslProcessingResult` — it orchestrates the entire pipeline that produces these objects.
- `FlightAstBuilderVisitor` creates all AST node types (`FlightPlanAst`, `LegAst`, `SegmentAst`, etc.) — the visitor traverses and directly constructs these representations.

### 12.3 Controller

**Principle:** Assign responsibility for handling system operations to a class that represents the overall system, a use-case scenario, or a root object in a façade.

**Where it appears:**

Every application-layer controller in `aisafe.core` is a GRASP Controller: it receives a system-level operation (e.g., "register aircraft", "import flight plan", "decommission aircraft"), coordinates the repositories and domain objects needed to carry it out, and returns a result to the UI layer. The controllers do not contain domain logic — they delegate to aggregates — and do not contain UI logic — the UI layer delegates to them.

Examples: `RegisterAircraftController`, `ImportFlightPlanController`, `DecommissionAircraftController`.

### 12.4 Low Coupling

**Principle:** Assign responsibilities so that coupling remains low. Prefer designs where a change in one class forces fewer changes in others.

**Where it appears:**

- The repository interface layer and constructor injection (section 5.5 DIP) are the primary mechanism: controllers depend on interfaces, so a backend swap requires no controller changes.
- Aggregates reference each other by identity value objects only (e.g., `FlightPlan` stores `aircraftId` as a string, not an `Aircraft` reference). This prevents inter-aggregate coupling at the object level and keeps each aggregate independently loadable.
- The DSL module (`aisafe.flight.dsl`) has no dependency on `aisafe.core` domain classes during parsing — it produces an intermediate `FlightDslDocument`, and only `ImportFlightPlanController` maps that to domain objects. This isolates the DSL pipeline from domain model evolution.

### 12.5 High Cohesion

**Principle:** Assign responsibilities so that cohesion remains high. A class should have a focused, related set of responsibilities.

**Where it appears:** The examples in section 5.1 (SRP) describe the same principle from a SOLID angle — `Aircraft` vs. `AircraftModel`, per-aggregate repository interfaces, and single-purpose DSL pipeline stages are all direct applications of High Cohesion. GRASP names the goal (cohesion); SRP names the heuristic (one reason to change); both point to the same design decisions in this codebase.

### 12.6 Polymorphism

**Principle:** When behaviour varies by type, assign responsibility for the variation to the types that exhibit it, using polymorphic operations rather than explicit type checks.

**Where it appears:**

- `JpaAircraftRepository` and `InMemoryAircraftRepository` both implement `AircraftRepository`. Controllers never branch on which implementation is present — they call the same interface methods and let the type handle the variation. The same pattern applies to all twelve repository pairs.
- `JpaRepositoryFactory` and `InMemoryRepositoryFactory` both implement `RepositoryFactory`. The application entry point selects one; everything downstream is polymorphic.
- The ANTLR-generated visitor and listener interfaces (`FlightDSLVisitor`, `FlightDSLListener`) define polymorphic traversal — concrete implementations (`FlightAstBuilderVisitor`, `FlightStructureListener`) handle specific node types through method override rather than `instanceof` chains.

### 12.7 Pure Fabrication

**Principle:** Assign a cohesive set of responsibilities to an artificial class (one that does not represent a domain concept) when doing so achieves low coupling and high cohesion that would otherwise be violated.

**Where it appears:**

- `BaseJpaRepositoryBase<T, K, I>` (section 6.3) is a textbook Pure Fabrication: it represents no air traffic concept and exists solely to absorb JPA boilerplate so concrete repositories stay lean.
- `InMemoryRepositoryFactory` is a fabrication — there is no real-world "in-memory factory" concept — created to allow the system to run without a database during development and testing.
- `AuthzConfigurator` and `AISafePasswordPolicy` are fabrications that exist to integrate the eapli.framework authentication infrastructure with the project's specific rules, with no domain meaning of their own.

### 12.8 Indirection

**Principle:** Assign responsibility to an intermediate object to mediate between components to avoid direct coupling.

**Where it appears:**

- `RepositoryFactory` is the indirection point between controllers and concrete persistence classes. Controllers never reference `JpaAircraftRepository` directly; they go through the factory.
- `FlightDslProcessor` introduces indirection between `ImportFlightPlanController` and the raw ANTLR lexer/parser. The controller is decoupled from ANTLR's API entirely.
- `AuthzRegistry` (eapli.framework) acts as indirection between controllers and the authentication/authorisation infrastructure — controllers call `authz.ensureAuthenticatedUserHasAnyOf(...)` without knowing the underlying session or token mechanism.

### 12.9 Protected Variations

**Principle:** Identify points of predicted variation or instability and assign responsibilities to create a stable interface around them.

**Where it appears:**

- The repository interface layer is the primary Protected Variations point in the architecture. Persistence technology (JPA, in-memory, and any future backend) is an identified variation point, and the repository interfaces create a stable boundary that shields the domain and application layers from it.
- The `RepositoryFactory` interface protects against variation in which persistence stack is active. Switching from H2 to PostgreSQL, or from JPA to any other ORM, requires changes only within `aisafe.persistence.impl`.
- The DSL `FlightDslDocument` / `FlightDslProcessingResult` intermediate representation protects `ImportFlightPlanController` from variation in the ANTLR grammar. If the grammar is extended, only the DSL module changes; the controller's `import(...)` method signature is stable.
- The `AISafeRoles` constants protect all controllers from variation in role string values — no controller embeds a raw role string literal.

### 12.10 GRASP Summary

| GRASP Pattern | Primary Location |
|---|---|
| Information Expert | Value objects (`Coordinates`, `RouteDesignation`), aggregate invariant methods |
| Creator | `FlightPlan` → `FlightLeg`, `RepositoryFactory`, `FlightDslProcessor` |
| Controller | All application-layer controllers in `aisafe.core` |
| Low Coupling | Repository interfaces, ID-only cross-aggregate references, DSL intermediate representation |
| High Cohesion | Single-responsibility aggregates, per-aggregate repository interfaces, DSL pipeline stages |
| Polymorphism | Repository interface pairs, `RepositoryFactory` implementations, ANTLR visitor/listener |
| Pure Fabrication | `BaseJpaRepositoryBase`, `InMemoryRepositoryFactory`, `AuthzConfigurator` |
| Indirection | `RepositoryFactory`, `FlightDslProcessor`, `AuthzRegistry` |
| Protected Variations | Repository interfaces, `RepositoryFactory`, DSL intermediate representation, `AISafeRoles` |
