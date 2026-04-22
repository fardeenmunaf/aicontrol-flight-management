# aisafe.core

**Shared Domain Kernel** — the heart of the AISafe system.

## Purpose

This module contains all **domain layer** code shared across the entire application, following Domain-Driven Design (DDD) principles:

- **Aggregates & Entities**: `Airport`, `AirControlArea`, `AircraftModel`, `FlightPlan`, `WeatherData`, `SystemUser`
- **Value Objects**: `Coordinate`, `Altitude`, `AirportCode`, `FlightPlanStatus`, etc.
- **Repository Interfaces**: Contracts for persistence (implemented in `aisafe.persistence.impl`)
- **Domain Events**: Events emitted by aggregates (e.g., `FlightPlanProposed`, `WeatherDataRegistered`)
- **Application Services**: Use case orchestration logic (US021–US085)
- **ANTLR DSL Grammar**: Flight path domain-specific language (`FlightPlanDSL.g4`)

## Dependencies

- `eapli.framework.core` — base DDD building blocks (AggregateRoot, ValueObject, DomainEvent, etc.)

## Notes

All other modules depend on this one. No UI, no persistence implementation, no framework-specific code belongs here.
