# US042 — Import Bulk Weather Data

## User Story

> As a **Weather Person**, I want to import bulk weather data into the system. Weather data might be from multiple external weather service providers — a simple CSV file format is available for initial development, and the system should be easy to expand to new weather data sources.

---

## Acceptance Criteria

| # | Criterion |
|---|-----------|
| AC01 | The system accepts a bulk file containing multiple weather data records for import |
| AC02 | A CSV file format is supported for initial development |
| AC03 | The import mechanism is extensible to additional providers/formats without changing the orchestration logic |
| AC04 | Each record is individually validated before being registered; invalid records do not abort the whole import |
| AC05 | The system reports how many records were imported successfully and how many failed, with error details |

---

## Implementation Overview

| Module | Role |
|--------|------|
| `aisafe.core` | `BulkImportWeatherDataController` (orchestration), `WeatherDataImporter` interface + `CSVWeatherDataImporter` / `XMLWeatherDataImporter` / `JSONWeatherDataImporter`, domain (`WeatherData`), `RegisterWeatherDataController` (per-record validation/persistence) |
| `aisafe.app.weather.client` | Console UI (`BulkImportWeatherUI`) — works in both `DIRECT_DB` and TCP client modes |
| `aisafe.persistence.impl` | `WeatherDataRepository` implementations |

---

## EAPLI Theoretical Principles — Practical Application

### Domain-Driven Design (DDD)

- `WeatherData` is the **Aggregate Root**; each imported record is mapped to a `WeatherDataDTO` and registered individually through `RegisterWeatherDataController`, which performs the domain validation (AC04).
- `WeatherDataDTO` is the **Anti-Corruption Layer** between external data formats (CSV/XML/JSON) and the domain model — importers never construct `WeatherData` directly.

### Gang of Four (GoF) Patterns

| Pattern | Location | Purpose |
|---------|----------|---------|
| **Strategy** | `WeatherDataImporter` interface, implemented by `CSVWeatherDataImporter`, `XMLWeatherDataImporter`, `JSONWeatherDataImporter` | `BulkImportWeatherDataController` is decoupled from the concrete data source/format (AC03) |
| **Facade** | `BulkImportWeatherDataController.importBulkWeatherData(...)` | Hides the per-record validation/registration loop and error accounting behind a single call |
| **DTO** | `WeatherDataDTO` | Transports parsed data from the importer to `RegisterWeatherDataController` without exposing domain internals |

### SOLID Principles

1. **Single Responsibility Principle (SRP)** — each importer only parses its own format; `BulkImportWeatherDataController` only orchestrates; `RegisterWeatherDataController` only validates/persists a single record.
2. **Open/Closed Principle (OCP)** — a new provider/format (e.g., a future external weather service) is added by implementing `WeatherDataImporter`, with no changes to `BulkImportWeatherDataController` (AC03).
3. **Liskov Substitution Principle (LSP)** — any `WeatherDataImporter` implementation can be passed to `importBulkWeatherData(...)` interchangeably.
4. **Interface Segregation Principle (ISP)** — `WeatherDataImporter` exposes only `importData()` and `getProviderName()`.
5. **Dependency Inversion Principle (DIP)** — the controller depends on the `WeatherDataImporter` abstraction, not on concrete parsers.

---

## CSV File Format (AC02)

`CSVWeatherDataImporter` reads semicolon-delimited files with 12 fields per line (an optional header line containing `ACA` is skipped):

```
ACA;Lat1;Long1;Lat2;Long2;Alt_inf;Alt_sup;Direction;Value;Date;Start_hour;End_hour
```

| Field | Description |
|---|---|
| `ACA` | Air Control Area code |
| `Lat1`, `Long1` | First coordinate pair (decimal degrees, `,` or `.` as decimal separator) |
| `Lat2`, `Long2` | Second coordinate pair |
| `Alt_inf`, `Alt_sup` | Altitude range, in feet |
| `Direction` | Wind direction, in degrees (0–360) |
| `Value` | Wind speed, in knots |
| `Date` | `dd/MM/yyyy` |
| `Start_hour`, `End_hour` | `HH:mm` |

Example:
```
121;43,840454;-9,795711;40,225;-7,9501;0;1000;90;28,75;22/06/2026;05:00;08:15
```

A malformed line raises `WeatherDataImporter.ImportException`, which carries the offending line number and aborts the whole file import (`BulkImportWeatherDataController` reports it as "Import failed at line N: ..."). A line that is well-formed but fails **domain** validation (e.g., out-of-range values) is instead reported per-record via `ImportResult.errors` — the rest of the file continues to be processed (AC04).

---

## User Interaction Flow

### Console (`aisafe.app.weather.client`)

1. Weather Person selects **Bulk Import Weather Data**.
2. The UI shows the expected CSV format and an example line.
3. The Weather Person enters the path to the CSV file.
4. - In `DIRECT_DB` mode: `CSVWeatherDataImporter` parses the file and `BulkImportWeatherDataController.importBulkWeatherData(...)` registers each record, returning an `ImportResult`.
   - In TCP client mode: the parsed records are serialized back to semicolon-delimited lines and uploaded to the weather server (`uploadWeather`), which performs the same import server-side.
5. The result (total processed, successes, errors with details, completed flag) is printed to the console.

---

## Key Classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `BulkImportWeatherUI` | `weather.presentation.weatherData` | Console UI: prompts for the CSV path, dispatches to local controller or TCP upload |
| `BulkImportWeatherDataController` | `weatherData.application` | Orchestrates the import: runs the importer, registers each `WeatherDataDTO`, accumulates an `ImportResult` |
| `BulkImportWeatherDataController.ImportResult` | `weatherData.application` | Value object with `providerName`, success/error counts, error messages and a `completed` flag |
| `WeatherDataImporter` | `weatherData.application.importer` | Strategy interface — `importData()` / `getProviderName()`; declares `ImportException` |
| `CSVWeatherDataImporter` | `weatherData.application.importer` | Parses the 12-field semicolon-delimited CSV format described above |
| `XMLWeatherDataImporter` | `weatherData.application.importer` | Alternative importer for XML-formatted weather data feeds |
| `JSONWeatherDataImporter` | `weatherData.application.importer` | Alternative importer for JSON-formatted weather data feeds |
| `RegisterWeatherDataController` | `weatherData.application` | Validates and persists a single `WeatherDataDTO` as a `WeatherData` aggregate |
| `WeatherDataDTO` | `weatherData.dto` | Format-agnostic transport object for one weather data record |

---

## Domain Notes

- The bulk importer never bypasses single-record validation: every parsed `WeatherDataDTO` goes through `RegisterWeatherDataController.registerWeatherData(...)`, the same path used by US041 (Register Weather Data) — there is a single source of truth for what makes a `WeatherData` record valid.
- A line-level `ImportException` (wrong number of fields, unparsable number/date/time) stops the import for that file and is reported with its line number; a record-level `IllegalArgumentException` (domain validation failure) is caught per record and does not stop the import (AC04/AC05).

---

## Testing Notes

`WeatherDataDTOTest`, `WeatherDataTest` and `WeatherDataControllerTest` cover the DTO, the `WeatherData` aggregate and `RegisterWeatherDataController`/`ListWeatherDataController`. There is currently **no dedicated unit test** for `BulkImportWeatherDataController` or for `CSVWeatherDataImporter`/`XMLWeatherDataImporter`/`JSONWeatherDataImporter` — adding these is recommended to lock in the CSV parsing rules and the per-record error accounting described above.

---

## Relação com outras User Stories

- **US041** — Register Weather Data: provides the single-record validation/persistence path reused by the bulk importer.
- **US043** — Consult Weather Data: the records imported here become visible through `ListWeatherDataController.listByDateAndArea(...)`.
