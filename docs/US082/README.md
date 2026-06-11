# US082 — Insert Weather Data in a Flight Plan

## User Story

> As a **Pilot**, I want to add weather data to a flight plan of mine. If the flight plan has been previously tested, the test is deemed void because of the new weather data.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC1 | Only the **pilot assigned** to a flight plan may add weather data to it. Attempts by other users must be rejected. |
| AC2 | Weather data may only be added to flight plans in **DRAFT or TESTED** status. Plans in VOID status must be rejected. |
| AC3 | Weather data is matched automatically per segment using three criteria (all must hold): **temporal** — the weather window overlaps the leg's departure–arrival time; **spatial** — at least one segment node coordinate falls inside the weather bounding box; **altitude** — the segment's altitude slot overlaps `[altitudeInf, altitudeSup]`. No manual selection is required. |
| AC4 | If the flight plan was previously **TESTED**, adding weather data automatically **voids the test** — the status reverts to DRAFT and the pilot is informed. |
| AC5 | The **supplement policy** is applied: new associations are added alongside existing ones. Previously associated weather records are never removed. |
| AC6 | Only an authenticated user with the **PILOT** role may perform this action. |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe.core` | `Segment` entity (stores weather IDs per segment), `FlightPlan` aggregate (status guard + delegation), `AddWeatherDataToFlightPlanController`, `AddWeatherResult` DTO |
| `aisafe.app.airtransport.console` | `AddWeatherDataUI` / `AddWeatherDataAction` — interactive console flow |
| `aisafe.app.javafx` | "Weather" button in `FlightPlanUI` action column — auto-adds and shows count |

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

- **Ubiquitous Language**: Terms like `addWeatherDataToSegment`, `voided`, `DRAFT`, `TESTED`, and `supplement` map directly to the domain vocabulary and are consistent across all layers.
- **Bounded Context**: Weather association belongs to the Flight Planning bounded context. `WeatherData` is only referenced by identity (UUID string) inside `Segment`, never by direct object reference — this preserves aggregate boundary integrity.
- **Aggregate Root**: `FlightPlan` is the aggregate root. The status transition (TESTED → DRAFT) is still controlled by the root via `addWeatherDataToSegment()`, which then delegates the actual storage to `Segment`. This ensures the invariant is always enforced through the root.
- **Value Objects**: `WeatherDataId` is an `@Embeddable` value object. `Segment` stores associations as a `Set<String>` via `@ElementCollection`, avoiding cross-aggregate JPA foreign keys.
- **Repository Pattern**: `AddWeatherDataToFlightPlanController` uses `FlightPlanRepository`, `WeatherDataRepository`, `AirportRepository`, and `NodeRepository` to load entities and persist state, with no coupling to JPA in the domain layer.

### Gang of Four (GoF) Patterns

| Pattern | Application | Benefit |
|---------|-------------|---------|
| **Data Transfer Object (DTO)** | `AddWeatherResult` carries `testWasVoided` and `addedCount` from the domain back to the presentation layer | The UI knows what to display without inspecting domain state directly |
| **Strategy** | The supplement policy is encapsulated in `Segment.addWeatherData()`; an alternative replace policy could be swapped in without touching the controller or UI | Policy changes are isolated |
| **Facade** | `AddWeatherDataToFlightPlanController` hides temporal/spatial/altitude matching, node resolution, and segment iteration behind a single `addAllRelevantWeatherData(plan)` call | Both UIs stay simple — one call, one result |
| **Template Method** | `findForWeatherUpdate` applies a fixed guard sequence (auth → existence → ownership → status) reused by both UIs | Guard logic is not duplicated |

### SOLID Principles

- **Single Responsibility Principle (SRP)**: `AddWeatherDataToFlightPlanController` handles auth and matching orchestration; `FlightPlan.addWeatherDataToSegment()` enforces the status invariant; `Segment.addWeatherData()` stores the association; the UIs handle presentation only.
- **Open/Closed Principle (OCP)**: The altitude filter was added without modifying the `FlightPlan` aggregate or the UI layers — only the controller's matching logic changed.
- **Liskov Substitution Principle (LSP)**: `FlightPlanRepository`, `WeatherDataRepository`, `AirportRepository`, and `NodeRepository` are all replaced by in-memory stubs in tests without breaking behaviour.
- **Interface Segregation Principle (ISP)**: `AddWeatherResult` exposes only `testWasVoided()` and `addedCount()` — callers are not forced to inspect plan or segment internals.
- **Dependency Inversion Principle (DIP)**: The controller depends on the repository interfaces, never on concrete JPA implementations.

---

## Domain Model Changes

### `Segment` — new field and methods

Weather data is now associated at **segment level**, not flight plan level. Each segment stores the IDs of the weather records that cover its position and altitude — this way the planned wind (`Segment.windSpeed` / `windDirection`) can be compared against the actual observed wind from the associated `WeatherData`.

```java
// Stored as strings to avoid cross-aggregate JPA foreign keys
@ElementCollection
@CollectionTable(name = "SegmentWeatherData",
                 joinColumns = @JoinColumn(name = "segment_id"))
@Column(name = "weather_data_id")
private Set<String> weatherDataIds = new LinkedHashSet<>();

public void addWeatherData(final WeatherDataId weatherDataId) {
    this.weatherDataIds.add(weatherDataId.toString());
}

public Set<String> weatherDataIds() {
    return Collections.unmodifiableSet(weatherDataIds);
}
```

### `FlightPlan` — updated method

The aggregate root keeps control of the status transition, then delegates storage to the segment:

```java
public boolean addWeatherDataToSegment(final Segment segment, final WeatherDataId weatherDataId) {
    if (this.status == FlightPlanStatus.VOID) {
        throw new IllegalStateException("Cannot add weather data to a VOID flight plan.");
    }
    final boolean wasVoided = this.status == FlightPlanStatus.TESTED;
    if (wasVoided) {
        this.status = FlightPlanStatus.DRAFT;
    }
    segment.addWeatherData(weatherDataId);
    return wasVoided;
}
```

---

## Status Transitions

```
DRAFT  ──[addWeatherDataToSegment]──▶ DRAFT   (no change; data added to segment)
TESTED ──[addWeatherDataToSegment]──▶ DRAFT   (test voided; data added to segment)
VOID   ──[addWeatherDataToSegment]──▶ (rejected — IllegalStateException)
```

---

## Relevance Check (AC3)

Weather records are matched automatically — no pilot selection required. For each `(segment, weatherData)` pair, all three criteria must hold:

```
For each leg in plan:
  │
  ├── 1. TEMPORAL — leg.departureTime < wd.date+endHour
  │              AND leg.arrivalTime  > wd.date+startHour
  │
  └── For each segment in leg:
        │
        ├── 2. ALTITUDE — segment.altitudeSlot overlaps [wd.altitudeInf, wd.altitudeSup]
        │
        └── 3. SPATIAL — startNode or endNode coordinate inside wd bounding box
                          (resolved via NodeRepository)
                              │
                              └── if match → plan.addWeatherDataToSegment(seg, wd)
```

The weather CSV data has different records for different geographic sub-areas and altitude bands within the same ACA zone. This three-way match ensures that:
- A climb segment at low altitude gets the weather for the low-altitude band at its position.
- A cruise segment at high altitude gets the weather for the high-altitude band at its position.
- A descend segment is matched independently.

---

## Supplement Policy (AC5)

New weather associations are **added alongside** existing ones — nothing is ever removed. This means:
- A segment can accumulate multiple weather records (e.g., from different flights or updates).
- Associating the same `WeatherDataId` twice is idempotent (it is stored in a `Set`).
- `addedCount` in `AddWeatherResult` reports only newly added records, skipping already-associated ones.

---

## DB Schema

```sql
CREATE TABLE SegmentWeatherData (
    segment_id      BIGINT       NOT NULL REFERENCES Segment(id),
    weather_data_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (segment_id, weather_data_id)
);
```

The old `FlightPlanWeatherData` table has been removed.

---

## User Interaction Flow

### Console UI

1. Pilot selects **"Add Weather Data to Flight Plan"** from the main menu.
2. All DRAFT and TESTED flight plans belonging to the pilot are listed.
3. Pilot selects a plan.
4. The system automatically searches for all weather records that match each segment (temporal + spatial + altitude) and associates them.
5. The system confirms:
   - **No matches**: `No weather data found that matches the time, position and altitude of this flight plan.`
   - **Matches found**: `N weather record(s) successfully associated with flight plan <designator>.`
   - **If plan was TESTED**: A voiding warning is shown before the success message.

### JavaFX UI

1. Pilot opens the **Flight Plans** screen.
2. Each DRAFT or TESTED DB plan row shows a **Weather** button in the Action column.
3. Clicking **Weather** triggers the automatic matching — no dialog or selection needed.
4. A result dialog is shown with the count of added records and, if applicable, the voiding warning.
5. The table refreshes automatically.

---

## Key Classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `Segment` | `aisafe.core` / `flightPlan.domain` | Stores weather IDs per segment (`SegmentWeatherData`); exposes planned wind for comparison |
| `FlightPlan` | `aisafe.core` / `flightPlan.domain` | Aggregate root; enforces VOID/TESTED status invariant via `addWeatherDataToSegment()` |
| `AddWeatherDataToFlightPlanController` | `aisafe.core` / `flightPlan.application` | Auth, ownership, status guards; temporal + spatial + altitude matching; iterates all segments |
| `AddWeatherResult` | `aisafe.core` / `flightPlan.application` | DTO carrying `testWasVoided` and `addedCount` to the presentation layer |
| `AddWeatherDataUI` | `aisafe.app.airtransport.console` | Interactive console — lists plans, triggers auto-add, shows count |
| `AddWeatherDataAction` | `aisafe.app.airtransport.console` | Console menu wiring — delegates to `AddWeatherDataUI.show()` |
| `FlightPlanUI` | `aisafe.app.javafx` | "Weather" button in action column; calls `addAllRelevantWeatherData`, shows result dialog |

---

## Test Coverage

| Test | What is verified |
|------|-----------------|
| `ensureRelevantWeatherIsLinkedToSegment` | Matching weather (temporal + spatial + altitude) is associated to the correct segment; `addedCount` = 1; plan stays DRAFT |
| `ensureAddingWeatherToTestedPlanVoidsTest` | Adding to a TESTED plan reverts status to DRAFT; `testWasVoided` is true |
| `ensureWeatherOutsideSpatialBoxIsNotAdded` | Weather whose bounding box does not contain any segment node is not added |
| `ensureWeatherAboveSegmentAltitudeIsNotAdded` | Weather whose altitude band is above the segment's altitude slot is not added |
| `ensureWeatherOutsideTimeWindowIsNotAdded` | Weather on a different date has no temporal overlap and is not added |
| `ensureAlreadyAssociatedWeatherIsNotDoubleAdded` | Running the auto-add twice does not create duplicate associations; second `addedCount` = 0 |
| `ensureOtherPilotCannotModifyPlan` | A pilot who does not own the plan gets `SecurityException` from `findForWeatherUpdate` |
| `ensureVoidPlanIsRejected` | `findForWeatherUpdate` throws `IllegalStateException` for VOID plans |
| `ensureEditablePlansReturnsOnlyDraftAndTested` | Only DRAFT and TESTED plans are returned for the logged-in pilot |
