# aisafe.infrastructure.application

**Application Infrastructure** — cross-cutting application concerns and infrastructure setup.

## Purpose

This module handles infrastructure configurations and setup logic that support the application, bridging the domain logic and external systems. Following Domain-Driven Design (DDD) principles and the structure of the `eapli.base` framework, this typically includes:

- **Application Context**: Initializing and providing access to application settings (e.g., `Application.java`, `AppSettings.java`).
- **Password Policies**: Implementations of security and password generation rules.
- **External Services Wiring**: Configuration and adapters for general external dependencies like messaging brokers or email clients, keeping the core domain clean.

## Dependencies

- `aisafe.core` — for domain models and application services that need to be configured.
- `eapli.framework.core` / `eapli.framework.infrastructure` — for base infrastructure classes and utilities.

## Notes

This module provides the necessary wiring and configuration to allow the application to run smoothly across different environments. It does not contain domain business logic (which belongs in `aisafe.core`) or specific UI components.
