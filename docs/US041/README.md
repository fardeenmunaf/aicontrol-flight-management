# US041 — Register Weather Data

## User Story

> As a **Weather Person**, I want to register weather data in the system for a specific air control area.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC01 | Only an authenticated **Weather Person** can register weather data |
| AC02 | Weather data must be associated with an existing air control area |
| AC03 | Required fields include: air control area code, date, start hour, end hour, latitude 1 & 2, longitude 1 & 2, altitude inferior & superior, wind direction and wind speed |
| AC04 | All numeric values (wind speed, coordinates, altitudes, direction) must be valid |
| AC05 | Date and time values must represent valid calendar values |
| AC06 | Registering weather data for an empty air control area code must be rejected with a meaningful error |

---

## Class Diagram

![Class Diagram](class-diagram.svg)

---

## Sequence Diagram — Manual Registration (Console)

![Sequence Diagram](sequence-register-weather.svg)

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe.app.backoffice.console` | Interactive console UI (`RegisterWeatherDataUI`, `RegisterWeatherDataAction`) |
| `aisafe.core` | Application controller (`RegisterWeatherDataController`) and domain model (`WeatherData`, `AirControlArea`) |
| `aisafe.infrastructure.application` | Repository implementation (`WeatherDataRepository`, `AirControlAreaRepository`) |

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

The design of US041 follows core DDD principles:

- **Ubiquitous Language**: Domain terms are used consistently (`WeatherData`, `AirControlArea`, `WindDirection`, `WindSpeed`, `Altitude`).
- **Bounded Context**: This feature is part of the Weather Management bounded context and is kept separate from other subdomains (Air Control, etc.).
- **Aggregate Root**: `WeatherData` acts as the aggregate root that encapsulates coherent meteorological information.
- **Value Objects**: Attributes such as `WindSpeed`, `Altitude` and `WeatherDataId` are modeled as immutable value objects to ensure consistency and clear semantics.
- **Repository Pattern**: `WeatherDataRepository` provides an abstraction over persistence, hiding implementation details.
- **Domain Events**: Events may be emitted when new weather data is registered (e.g., `WeatherDataRegisteredEvent`).

### Gang of Four (GoF) Patterns

Several classic design patterns are applied:

| Pattern | Location | Purpose |
|--------|-------|----------|
| **Factory** | `WeatherData` constructor | Builds `WeatherData` aggregates with encapsulated validation |
| **Repository** | `WeatherDataRepository` | Persistence abstraction; eases unit testing and DB changes |
| **Strategy** | `AuthorizationService` | Different authorization strategies per role (WEATHER_PERSON, etc.) |
| **Data Transfer Object (DTO)** | `WeatherDataDTO` | Carries data between UI and Application Layer, decoupling presentation from logic |
| **Adapter** | Console UI → Controller | Adapts user input into application commands |

### SOLID Principles

The architecture in US041 respects the five SOLID principles:

1. **Single Responsibility Principle (SRP)**
   - `RegisterWeatherDataUI`: presentation only
   - `RegisterWeatherDataController`: application logic and coordination
   - `WeatherData`: domain rules for meteorological data
   - Each class has a single reason to change

2. **Open/Closed Principle (OCP)**
   - `AuthorizationService` can be extended with new roles without modifying its existing client code

3. **Liskov Substitution Principle (LSP)**
   - `WeatherDataRepository` implements a consistent interface; any implementation (JPA, in-memory, etc.) can be substituted without breaking client code

4. **Interface Segregation Principle (ISP)**
   - `WeatherDataRepository` exposes only required operations; minimal interfaces exist for specific roles (e.g., `WeatherPersonAuthorization`)

5. **Dependency Inversion Principle (DIP)**
   - Controllers depend on abstractions (`AuthorizationService`, `WeatherDataRepository`) rather than concrete implementations, easing testing with mocks/stubs

### Data Transfer Objects (DTOs)

DTOs isolate the presentation layer from domain logic:

- **`WeatherDataDTO`**
  - Contains raw user input (strings, primitive numbers)
  - Transfers data from the UI to the Controller

**Benefit**: The presentation layer never manipulates domain entities directly, reducing coupling and making testing easier.

### Factory Pattern

The Factory pattern (implemented via domain constructor) encapsulates complex aggregate creation:

```java
// WeatherData constructor validates all parameters
WeatherData data = new WeatherData(
        dto.acaCode(),
        dto.latitude1(),
        dto.longitude1(),
        dto.latitude2(),
        dto.longitude2(),
        dto.altitudeInf(),
        dto.altitudeSup(),
        dto.direction(),
        dto.windSpeed(),
        dto.date(),
        dto.startHour(),
        dto.endHour());
```

Factory responsibilities:
- Validate that all parameters are coherent
- Ensure the aggregate is created in a valid state
- Encapsulate complex initialization logic
- Make it easier to add new parameters without changing client code

## User Interaction Flow

1. Weather Person logs in and navigates to **Register Weather Data** from the Weather menu.
2. The console prompts for:
   - Air Control Area Code
   - Coordinates (Latitude 1 & 2, Longitude 1 & 2)
   - Altitudes (Inferior & Superior)
   - Date (YYYY-MM-DD)
   - Start and End Hour (HH:MM:SS format)
   - Wind Direction (0-360 degrees)
   - Wind Speed (m/s, non-negative)
3. `RegisterWeatherDataController` verifies the caller has the `WEATHER_PERSON` role via `AuthorizationService`.
4. The controller validates:
   - Air Control Area code is not empty
   - All numeric fields are within valid ranges
   - Date and hours are valid
5. A domain `WeatherData` entity is created and saved via `WeatherDataRepository`.
6. The console prints `Weather data registered successfully.`

---

## Key Classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `RegisterWeatherDataUI` | `backoffice.presentation.weather` | Console form — collects input, delegates to controller |
| `RegisterWeatherDataAction` | `backoffice.presentation.weather` | Menu action wiring — instantiates and shows `RegisterWeatherDataUI` |
| `RegisterWeatherDataController` | `weatherData.application` | Enforces WEATHER_PERSON authorization, validates weather data, persists it |
| `WeatherData` | `weatherData.domain` | Aggregate root — holds air control area, timestamp, wind data, etc. |
| `WeatherDataId` | `shared.domain` | Value object used as the aggregate identity |
| `WeatherDataDTO` | `weatherData.dto` | DTO used to transfer data from UI to Controller |
| `WeatherDataRepository` | `weatherData.repository` | Domain repository interface for persisting and querying weather data |

---

## Test Cases

### TC01 – Successful weather data registration
**Given** an authenticated Weather Person and an existing air control area  
**When** they submit valid weather data (wind direction, speed, temperature, pressure, date/time)  
**Then** the system saves the weather data and confirms success

### TC02 – Registration for non-existent air control area rejected
**Given** an operator references a non-existent air control area  
**When** the form is submitted  
**Then** the system rejects the registration with a meaningful error message

### TC03 – Invalid numeric values rejected
**Given** a Weather Person submits a negative wind speed value  
**When** the form is submitted  
**Then** the system rejects the registration with a validation error

### TC04 – Invalid date/time rejected
**Given** a Weather Person submits an invalid date (e.g., 2026-13-45)  
**When** the form is submitted  
**Then** the system rejects the registration with an appropriate error

### TC05 – Missing required fields rejected
**Given** a Weather Person submits weather data without specifying wind direction  
**When** the form is submitted  
**Then** the system rejects the registration and indicates the missing field

### TC06 – Weather data consultable after registration
**Given** weather data has been successfully registered for an area on a given date  
**When** a Weather Person, Pilot, or FCO consults weather data (US062) for that area and date  
**Then** the registered data is returned correctly

### TC07 – Non-Weather Person cannot register weather data
**Given** an authenticated user with role Pilot  
**When** they attempt to register weather data  
**Then** the system denies access with a 403/Forbidden error

---

## Done Concept

- A Weather Person can register weather data for a specific air control area
- Required fields are provided: air control area, date/time, and weather parameters (e.g., wind direction, wind speed, temperature, pressure)
- The air control area must already exist in the system
- Only authenticated Weather Persons can perform this action
- Test coverage of controller and domain packages ≥ 90%
- Code compiles and all tests pass
- Technical documentation updated in the repository (analysis, design, tests) in Markdown format
