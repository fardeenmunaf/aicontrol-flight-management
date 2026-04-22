# aisafe.app.backoffice.console

**Backoffice Console Application** — for Administrators and Operators.

## Purpose

This module is the **console UI application** for system administrators and backoffice operators. It covers:

### Backoffice Admin (US021–US023)
- Register new backoffice users
- List all system users
- Activate / deactivate user accounts

### Backoffice Operator (US050–US054)
- Create and manage **Airports**
- Create and manage **Air Control Areas**
- Create and manage **Aircraft Models**
- Register base infrastructure data

## Structure

```
presentation/
├── MainMenu.java
├── user/       ← user management UIs
├── airport/    ← airport management UIs
├── aircraft/   ← aircraft model UIs
└── aircontrolarea/ ← control area UIs
BackofficeApp.java  ← main() entry point
```

## Dependencies

- `aisafe.core` — application services and domain model
- `aisafe.persistence.impl` — JPA repositories
- `eapli.framework.infrastructure.authz` — role-based access control
