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

## Configuration

The active persistence unit defaults to in-memory H2 (`aisafe-pu`). You can switch to PostgreSQL (`aisafe-pu-postgres`) or override JDBC settings using system properties.

Supported system properties (read by `jpa.PersistenceSettings`):

- `aisafe.persistence.unit`
- `aisafe.db.dialect`
- `aisafe.db.driver`
- `aisafe.db.url`
- `aisafe.db.username`
- `aisafe.db.password`

Examples (bash):

```bash
export AISAFE_PERSISTENCE_UNIT=aisafe-pu-postgres
export AISAFE_DB_URL=jdbc:postgresql://localhost:5432/aisafe
export AISAFE_DB_USERNAME=postgres
export AISAFE_DB_PASSWORD=postgres
./run-bootstrap.sh
./run-backoffice.sh
```

