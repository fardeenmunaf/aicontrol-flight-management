## Plan: Analyze and Utilize eapli.base Template

The `eapli.base` template is a foundational Domain-Driven Design (DDD) project utilizing the EAPLI framework. It provides a pre-configured Clean Architecture blueprint, setting up core domain modeling, JPA persistence, role-based console applications, and data bootstrapping, which we adapt as a starting point for the new `aisafe` LAPR4 project.

### Steps
1. Review `exemplo.core` for base domain concepts, aggregates, and value objects.
2. Analyze `exemplo.persistence.impl` for JPA/Hibernate configurations and repository implementations.
3. Explore the UI modules like `exemplo.app.backoffice.console` to understand role-specific interface patterns.
4. Examine `exemplo.app.bootstrap` to see how initial mock data and system settings are populated.
5. Configure authorization and event handling mechanisms natively provided via `eapli.framework`.
6. Refactor the `exemplo.*` namespace to the specific project namespace (e.g., `aisafe.*`).

### Further Considerations
1. Bulk rename packages: Should we execute a script to rename all `exemplo` packages to match our specific project architecture?
2. Entity Mapping: Do we have specific constraints for JPA or specific database providers (H2 vs PostgreSQL) we need to align config with?
