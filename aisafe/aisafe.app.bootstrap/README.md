# aisafe.app.bootstrap

**System Bootstrapper** — seed data for development and testing.

## Purpose

This module **populates the system with initial mock data** on first run, enabling the rest of the application to be tested without manual data entry. It is the equivalent of the `exemplo.app.bootstrap` module in the eapli.base template.

Typical bootstrapped data includes:
- Default **system users** (admin, operator, FCO, pilot accounts)
- Sample **Airports** (with ICAO codes and coordinates)
- Sample **Air Control Areas**
- Sample **Aircraft Models**
- Sample **Flight Plans** and **Weather Data** for simulation testing

## How it runs

- Runs once at startup (or on demand during development)
- Uses the application services from `aisafe.core` to create entities through the proper domain layer
- Is **not** deployed in production — only used locally and in CI test environments

## Dependencies

- `aisafe.core` — application services
- `aisafe.persistence.impl` — JPA repositories
