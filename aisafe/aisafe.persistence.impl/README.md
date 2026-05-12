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

## Profiles (NFR08)

This module supports **Spring-style profiles** to switch the database **without any user prompt**.
The default profile is set in `application.properties`:

- `spring.profiles.default=dev`

### Dev (H2 in-memory)

Uses `application-dev.properties` and the persistence unit `aisafe-pu`.

### Prod (PostgreSQL)

Uses `application-prod.properties` and the persistence unit `aisafe-pu-postgres`.

### Run examples

- Default (dev/H2): start any app normally.
- Production (PostgreSQL): pass `--spring.profiles.active=prod` when starting the app.

Example:

```
java -jar app.jar --spring.profiles.active=prod
```

No menu or prompt is displayed; the active profile fully determines the database.

## Notes

No business logic here. Only persistence mechanics — no application module should depend on this directly; dependency injection wires implementations at runtime.
