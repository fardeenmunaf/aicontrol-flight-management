# aisafe.app.airtransport.console

**Air Transport Company Console Application** — for Pilots and Company Collaborators.

## Purpose

This module is the **console UI application** for air transport companies. It handles fleet management and flight plan lifecycle:

### Air Transport Company Collaborator (US070–US085)
- Manage company **fleet** (register/list aircraft — US070–US071)
- **Propose** new flight plans using the DSL (US080)
- **Cancel** proposed flight plans (US081)
- Parse and validate flight plan paths via the ANTLR DSL (US084)
- Submit flight plans for C-based validation (US085)

### Pilot
- Review and test flight plans remotely
- Communicates with the server via **TCP/IP**

## Structure

```
presentation/
├── MainMenu.java
├── fleet/          ← fleet management UIs
└── flightplan/     ← propose / cancel / view flight plan UIs
AirTransportApp.java  ← main() entry point
```

## Dependencies

- `aisafe.core` — domain model + application services
- `aisafe.persistence.impl` — JPA repositories
- ANTLR4 runtime — for parsing flight plan DSL input
