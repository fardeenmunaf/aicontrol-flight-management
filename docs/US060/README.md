# US060 — Register Air Transport Company

## User Story

> As a **Backoffice Operator**, I want to register an air transport company.
> A company has a company name, an IATA code (2 letters) and ICAO code (2–3 letters) that must be unique.
> This must also be achievable by a bootstrap process.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC1 | Only an authenticated **Backoffice Operator** may register a company. |
| AC2 | IATA code must be exactly **2 letters** (A–Z). |
| AC3 | ICAO code must be **2–3 letters** (A–Z). |
| AC4 | Company name must be non-empty. |
| AC5 | IATA and ICAO codes must each be **unique** — duplicate codes are rejected. |
| AC6 | The bootstrap process registers seed companies if they do not already exist (idempotent). |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe.app.backoffice.console` | Interactive console UI (`RegisterAirTransportCompanyUI`, `RegisterAirTransportCompanyAction`) |
| `aisafe.core` | Application controller (`RegisterAirTransportCompanyController`) and domain model (`AirTransportCompany`) |
| `aisafe.app.bootstrap` | Seed data (`AirTransportCompanyBootstrap`, `AISafeBootstrap`) |

---

## Class Diagram

![Class Diagram](class-diagram.svg)

---

## Sequence Diagram — Manual Registration (Console)

![Sequence Diagram](sequence-register-air-transport-company.svg)

---

## User Interaction Flow

1. Backoffice Operator navigates to **Register Air Transport Company** from the menu.
2. The console prompts for:
   - Company name (e.g. `TAP Air Portugal`)
   - IATA code — exactly 2 letters (e.g. `TP`)
   - ICAO code — 2–3 letters (e.g. `TAP`)
3. `RegisterAirTransportCompanyController` verifies the caller has the `BACKOFFICE_OPERATOR` role.
4. The controller validates IATA and ICAO format, then checks uniqueness against the repository.
5. A new `AirTransportCompany` is created (with a generated `AirTransportCompanyId`) and saved.
6. The console prints `Air Transport Company registered successfully.`

---

## Bootstrap Process

`AISafeBootstrap.main()` calls `AirTransportCompanyBootstrap.execute()`, which seeds the following companies if they do not already exist:

| Company | IATA | ICAO |
|---------|------|------|
| TAP Air Portugal | TP | TAP |
| RyanAir | FR | RYR |
| EasyJet | U2 | EZY |
| Lufthansa | LH | DLH |
| Air France | AF | AFR |
| British Airways | BA | BAW |
| KLM Royal Dutch Airlines | KL | KLM |
| Iberia | IB | IBE |
| Alitalia | AZ | AZA |
| Swiss International Airlines | LX | SWR |

Uniqueness is checked by IATA and ICAO before each insert — re-running bootstrap does not create duplicates.

---

## Key Classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `RegisterAirTransportCompanyUI` | `backoffice.presentation.airtransportcompany` | Console form — collects name, IATA, ICAO and delegates to controller |
| `RegisterAirTransportCompanyAction` | `backoffice.presentation.airtransportcompany` | Menu action wiring — instantiates and shows `RegisterAirTransportCompanyUI` |
| `RegisterAirTransportCompanyController` | `airtransportcompany.application` | Enforces BACKOFFICE_OPERATOR authorization, validates codes, checks uniqueness, persists |
| `AirTransportCompany` | `airtransportcompany.domain` | Aggregate root — holds name, IATA code, ICAO code and a generated identity |
| `AirTransportCompanyId` | `shared.domain` | UUID-based value object used as the aggregate identity |
| `AirTransportCompanyRepository` | `airtransportcompany.repositories` | Domain repository — `findByIataCode`, `findByIcaoCode`, `save` |
| `AirTransportCompanyBootstrap` | `bootstrap` | Seeds 10 default companies on first run; idempotent |
| `AISafeBootstrap` | `bootstrap` | Application bootstrap entry point — orchestrates all seed data loaders |
