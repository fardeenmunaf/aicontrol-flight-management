# US043 — Consult Weather Data

## User Story

> As a **Weather Person**, a **Pilot**, or a **Flight Control** operator, I want to consult weather data in the system for a given day and in a specific air control area.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC01 | A Weather Person, Pilot or Flight Control operator can consult weather data |
| AC02 | The consultation can be filtered by **air control area (ACA)** |
| AC03 | The consultation can be filtered by **date** |
| AC04 | Leaving the area and/or date filter blank returns weather data for all areas and/or all dates |
| AC05 | Each returned record shows the area, coordinates, altitude range, wind direction/speed, date and time window |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe.core` | `ListWeatherDataController` (query logic), `WeatherDataRepository` (`findByAirControlArea`, `findAll`), domain `WeatherData`, `WeatherDataDTO` |
| `aisafe.app.backoffice.console` | Console UI (`ViewWeatherDataByDayAreaUI`), registered in the backoffice `MainMenu` as "Weather Data (by Day & Area)" |
| `aisafe.app.weather.client` | Console UI (`ViewWeatherDataUI`) for the Weather Person / TCP weather client |
| `aisafe.app.javafx` | `WeatherClientUI` — JavaFX consultation view |
| `aisafe.persistence.impl` | `WeatherDataRepository` implementations |

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

- `WeatherData` is the **Aggregate Root**; `ListWeatherDataController` only reads aggregates through `WeatherDataRepository` and maps them to `WeatherDataDTO` — no domain state is mutated by a consultation.
- `WeatherDataDTO` decouples the presentation layers (console, JavaFX, TCP client) from the JPA-mapped `WeatherData` aggregate.

### Gang of Four (GoF) Patterns

| Pattern | Location | Purpose |
|---------|----------|---------|
| **Repository** | `WeatherDataRepository.findByAirControlArea(area)` / `findAll()` | Encapsulates the query strategy used by `listByDateAndArea` |
| **DTO** | `WeatherDataDTO` | Read-only projection of `WeatherData` returned to every presentation layer |

### SOLID Principles

1. **Single Responsibility Principle (SRP)** — `ListWeatherDataController` is responsible only for querying/mapping; filtering by date is applied in the controller, filtering by area is delegated to the repository.
2. **Open/Closed Principle (OCP)** — additional filters (e.g., by altitude range) can be added to `listByDateAndArea` without changing the repository contract.
3. **Liskov Substitution Principle (LSP)** — any `WeatherDataRepository` implementation (JPA or in-memory) is substitutable.
4. **Interface Segregation Principle (ISP)** — `WeatherDataRepository` exposes only the two query methods needed by the consultation use cases (`findByAirControlAreaAndDateTime`, `findByAirControlArea`) plus the generic `DomainRepository` operations.
5. **Dependency Inversion Principle (DIP)** — all presentation layers depend on `ListWeatherDataController`/`WeatherDataRepository` abstractions, not on the persistence implementation.

---

## Query Logic (`ListWeatherDataController.listByDateAndArea`)

```
listByDateAndArea(area, date):
    if area is null or blank:
        candidates = WeatherDataRepository.findAll()          // AC04 — all areas
    else:
        candidates = WeatherDataRepository.findByAirControlArea(area)  // AC02

    for wd in candidates:
        if date is null or wd.date() == date:                 // AC03 / AC04 — all dates
            include wd (mapped to WeatherDataDTO)
```

This single method serves AC02–AC04: omitting `area`, `date`, or both progressively widens the result set to "all areas" / "all dates" / everything.

---

## User Interaction Flow

### Backoffice Console (`ViewWeatherDataByDayAreaUI`)

1. Weather Person / Flight Control operator selects **Weather Data (by Day & Area)** from the backoffice main menu.
2. Enters the ACA code (optional — blank means all areas).
3. Enters the date in `dd/MM/yyyy` (optional — blank means all dates); an invalid format aborts with an error message.
4. The system lists every matching record: ACA, both coordinate pairs, altitude range, wind direction/speed, date and time window — or "No weather data found for the given filters." if the result is empty.

### Weather Client (`ViewWeatherDataUI`) / JavaFX (`WeatherClientUI`)

Pilots and Weather Persons using the dedicated weather client (console or JavaFX) get the same `listByDateAndArea` query, presented in the client's own format — used when consulting weather data ahead of validating a flight plan (US085).

---

## Key Classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `ViewWeatherDataByDayAreaUI` | `backoffice.presentation.weather` | Backoffice console UI — prompts for ACA + date filters |
| `ViewWeatherDataUI` | `weather.presentation.weatherData` | Weather client console UI for the same query |
| `WeatherClientUI` | `javafxui.presentation.weather` | JavaFX consultation view |
| `ListWeatherDataController` | `weatherData.application` | `listByDateAndArea(area, date)` — implements AC02–AC04; `listAll()` for unfiltered listings |
| `WeatherDataRepository` | `weatherData.repository` | `findByAirControlArea(String)`, `findByAirControlAreaAndDateTime(...)`, plus generic `DomainRepository` operations |
| `WeatherData` | `weatherData.domain` | Aggregate Root holding ACA code, coordinates, altitude range, wind direction/speed, date and time window |
| `WeatherDataDTO` | `weatherData.dto` | Read-only projection returned to all presentation layers |

---

## Testing Notes

`WeatherDataControllerTest` covers `ListWeatherDataController` (including `listByDateAndArea`) alongside `RegisterWeatherDataController`.

---

## Relação com outras User Stories

- **US041** — Register Weather Data: produces the records consulted here.
- **US042** — Import Bulk Weather Data: bulk-imported records become consultable through the same `listByDateAndArea` query.
- **US085** — Test/validate a flight plan: pilots typically consult weather data for the relevant ACAs before/while validating a flight plan.
