# US072 — List Aircraft Fleet

## User Story

> As an **Air Transport Company Collaborator**, I want to list my company's aircraft fleet.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC01 | Only an authenticated **ATCC** can list their company's fleet |
| AC02 | Each entry shows at minimum: registration number, model, operational status, and cabin config |
| AC03 | The list includes all aircraft regardless of operational status |
| AC04 | An ATCC cannot view another company's fleet |
| AC05 | If the company has no aircraft, the system displays an appropriate empty-state message |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe.app.airtransport.console` | Interactive console UI (`ListFleetUI`, `ListFleetAction`) |
| `aisafe.core` | Application controller (`ListFleetController`) and domain model (`Aircraft`, `AircraftModel`, `AirTransportCompany`) |
| `aisafe.infrastructure.application` | Repository implementation (`AircraftRepository`, `AirTransportCompanyRepository`) |

---

## User Interaction Flow

1. Air Transport Company Collaborator (ATCC) logs in and navigates to **List Aircraft Fleet** from the Fleet Management menu.
2. `ListFleetController` verifies the caller has the `AIR_TRANSPORT_COMPANY_COLLABORATOR` role via `AuthorizationService`.
3. The controller retrieves the authenticated user's associated company (via email domain or explicit association).
4. All aircraft registered to that company are retrieved via `AircraftRepository.findByCompany()`.
5. For each aircraft, the system displays:
   - Registration Number (unique identifier)
   - Model Name and Manufacturer
   - Operational Status (active, maintenance, retired, etc.)
   - Cabin Configuration (economy, business, cargo, mixed, etc.)
6. If no aircraft exist, the system displays: `"No aircraft registered for your company."`
7. The list is presented in console table format, sorted by registration number.

---

## Key Classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `ListFleetUI` | `airtransport.presentation.fleet` | Console form — displays fleet table, handles user navigation |
| `ListFleetAction` | `airtransport.presentation.fleet` | Menu action wiring — instantiates and shows `ListFleetUI` |
| `ListFleetController` | `aircraft.application` | Enforces ATCC authorization, retrieves authenticated user's company, retrieves fleet data |
| `Aircraft` | `aircraft.domain` | Aggregate root — holds registration number, model, status, cabin config, owner company |
| `AircraftStatus` | `aircraft.domain` | Enum: ACTIVE, MAINTENANCE, RETIRED, GROUNDED, etc. |
| `CabinConfiguration` | `aircraft.domain` | Value object or enum describing cabin layout (economy seats, business seats, cargo volume, etc.) |
| `AirTransportCompany` | `company.domain` | Referenced entity — company that owns the aircraft |
| `AircraftRepository` | `aircraft.repository` | Domain repository for querying aircraft by company |
| `AirTransportCompanyRepository` | `company.repository` | Domain repository for querying company details |

---

## Class Diagram

![Class Diagram](class-diagram.svg)

---

## Sequence Diagram — List Aircraft Fleet (Console)

![Sequence Diagram](sequence-list-fleet.svg)

---

## Authorization

- **Allowed Roles**: AIR_TRANSPORT_COMPANY_COLLABORATOR
- **Ownership Check**: ATCC can only view aircraft belonging to their own company
- **Isolation**: Each ATCC automatically sees only their company's fleet; cross-company access is denied with error: `"Access denied. You can only view your company's fleet."`

---

## Data Presentation

### Fleet Table Format

```
╔════════════════╦══════════════════════════╦════════════════════╦════════════════════╗
║ Registration # ║ Model                    ║ Status             ║ Cabin Configuration║
╠════════════════╬══════════════════════════╬════════════════════╬════════════════════╣
║ CS-TPA         ║ Airbus A320 (Airbus)     ║ Active             ║ 180 Economy        ║
║ CS-TPB         ║ Embraer E190 (Embraer)   ║ Active             ║ 118 Economy        ║
║ CS-TPC         ║ Airbus A330 (Airbus)     ║ Maintenance        ║ 375 Mixed          ║
║ CS-TPD         ║ Boeing 737 (Boeing)      ║ Retired            ║ 162 Economy        ║
╚════════════════╩══════════════════════════╩════════════════════╩════════════════════╝
```

### Empty State Message

```
No aircraft registered for your company.
```

---

## Test Cases

### TC01 – Successfully list company fleet
**Given** an authenticated ATCC and a company with multiple aircraft  
**When** they request the fleet list  
**Then** the system returns all aircraft with registration, model, status, and cabin config

### TC02 – Empty fleet message
**Given** a company has no aircraft registered  
**When** the ATCC requests the fleet list  
**Then** the system displays an appropriate empty-state message

### TC03 – Fleet list includes retired aircraft
**Given** a company has both active and retired aircraft  
**When** the ATCC lists the fleet  
**Then** both active and retired aircraft appear with their correct status

### TC04 – ATCC cannot view another company's fleet
**Given** an authenticated ATCC from company A  
**When** they attempt to list the fleet of company B  
**Then** the system denies access with an appropriate error

### TC05 – Non-ATCC cannot list fleet
**Given** an authenticated user with role FCO  
**When** they attempt to list an air transport company's fleet  
**Then** the system denies access with a 403/Forbidden error

---

## Done Concept

- An ATCC can list all aircraft in their company's fleet
- Each entry displays at minimum: registration number, model, operational status, and cabin configuration
- The list includes aircraft regardless of operational status (active or retired)
- Only authenticated ATCCs can view their own company's fleet
- Test coverage of controller and domain packages ≥ 90%
- Code compiles and all tests pass
- Technical documentation updated in the repository (analysis, design, tests) in Markdown format

