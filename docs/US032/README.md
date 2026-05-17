# US032 — Disable/Enable Users

## User Story

> As an **Administrator**, I want to be able to disable/enable users of the backoffice.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC1 | Only an authenticated **Admin** may disable or enable users. |
| AC2 | The administrator selects the target user from a list showing current status. |
| AC3 | The operation is confirmed before executing. |
| AC4 | Disabling a user deactivates them in both the auth framework and the domain model. |
| AC5 | Enabling a user reactivates them in both the auth framework and the domain model. |
| AC6 | The operation is cancelled if the administrator does not confirm. |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe.app.backoffice.console` | Interactive console UI (`DisableEnableUserUI`, `DisableEnableUserAction`) |
| `aisafe.core` | Application controller (`DisableEnableUserController`) and domain model (`User`) |

---

## Class Diagram

![Class Diagram](class-diagram.svg)

---

## Sequence Diagram

![Sequence Diagram](sequence-disable-enable-user.svg)

---

## User Interaction Flow

1. Administrator logs in and navigates to **Disable/Enable User** from the Users menu.
2. The console fetches and displays all users with their current status.
3. The administrator selects a user from the list.
4. The console asks for confirmation: `Are you sure you want to disable/enable this user? (Y/N)`.
5. On confirmation:
   - If the user is **ACTIVE** → `disableUser(email)` is called:
     - The auth framework user is deactivated via `AuthzRegistry.userService().deactivateUser(…)`.
     - The domain `User` calls `disable()` and is saved — status set to `DISABLED`.
     - Console prints `User disabled successfully.`
   - If the user is **DISABLED** → `enableUser(email)` is called:
     - The auth framework user is reactivated via `AuthzRegistry.userService().activateUser(…)`.
     - The domain `User` calls `enable()` and is saved — status set to `ACTIVE`.
     - Console prints `User enabled successfully.`
6. If the administrator answers **N**, the operation is cancelled with no changes.

---

## Key Classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `DisableEnableUserUI` | `backoffice.presentation.user` | Displays user list, captures selection and confirmation, delegates to controller |
| `DisableEnableUserAction` | `backoffice.presentation.user` | Menu action wiring — instantiates and shows `DisableEnableUserUI` |
| `UserPrinter` | `backoffice.presentation.user` | Visitor that formats a `User` for display in the selection widget |
| `DisableEnableUserController` | `user.application` | Enforces ADMIN authorization; updates both auth framework and domain repository |
| `User` | `user.domain` | Aggregate root — `enable()` sets status to `ACTIVE`, `disable()` sets status to `DISABLED` |
| `UserRepository` | `user.repository` | Domain repository interface for querying and persisting users |
