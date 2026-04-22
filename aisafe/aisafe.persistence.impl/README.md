# aisafe.persistence.impl

**JPA/Hibernate Repository Implementations**.

## Purpose

This module provides the **infrastructure layer** for data persistence. It implements the repository interfaces defined in `aisafe.core` using JPA + Hibernate:

- `JpaAirportRepository`
- `JpaAirControlAreaRepository`
- `JpaAircraftModelRepository`
- `JpaFlightPlanRepository`
- `JpaWeatherDataRepository`
- `JpaUserRepository`

Also contains:
- `persistence.xml` — JPA configuration (datasource, dialect, entity scanning)
- `PersistenceContext.java` — singleton context/factory helper

## Dependencies

- `aisafe.core` — repository interfaces and domain entities
- `eapli.framework.infrastructure.persistence` — base JPA infrastructure
- Hibernate ORM
- H2 (dev) / PostgreSQL (prod)

## Notes

No business logic here. Only persistence mechanics — no application module should depend on this directly; dependency injection wires implementations at runtime.
