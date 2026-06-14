# Project AIControl

## 1. Description of the Project

Project AIControl is a prototype software solution designed for flight management and air traffic control simulation. The system manages complex aviation operations, validates flight plans, and simulates air traffic to ensure safety and prevent mid-air collisions.

This integrated system is composed of several core modules:
* **Backoffice Configuration:** Management of aviation assets, including aircraft models, engine models, airports, air transport companies, and flight control areas.
* **Flight Management & DSL:** Creation, parsing, and semantic validation of flight plans using a custom Domain Specific Language (Core Flight DSL) processed via ANTLR.
* **Air Traffic Simulation:** A highly scalable, multi-process, and multi-threaded simulation engine developed in C. It tracks aircraft trajectories, applies aerodynamic physics (Lift and Drag), and detects safety violations in real-time.
* **Weather Integration:** A TCP-based weather data service that allows weather persons to upload atmospheric conditions affecting air control areas.
* **Pilot Remote Access:** A TCP server/client pair that allows pilots to remotely review and test flight plans.
* **Remote Accesses Logging:** A standalone logging server (with optional HTTPS status page) that records remote access events.
* **JavaFX Dashboard:** A graphical dashboard for real-time visualization of simulation and weather data.

## 2. Planning and Technical Documentation

[Planning and Technical Documentation](docs/readme.md)

## 3. How to Build

Before running the project, you must compile all modules. Use the appropriate script for your operating system:

**Windows:**
```bat
rebuild-all.bat
```

**Linux / macOS:**
```sh
./rebuild-all.sh
```

> **Tip:** `rebuild-all` runs `mvn clean package` (compiles, runs tests, packages). Use `build-all.sh` / `build-all.bat` instead for a faster incremental rebuild that skips tests.

This compiles and packages all Java modules and copies their runtime dependencies into each module's `target/dependency/` folder so the run scripts can build the classpath.

---

## 4. How to Execute Tests

### Prerequisites

- Java 21 installed
- Project built at least once (see **Section 3 — How to Build**)

### Run All Tests (from project root)

**Linux / macOS:**
```sh
cd aisafe
mvn test
```

**Windows:**
```bat
cd aisafe
mvn test
```

### Run Tests for a Specific Module

```sh
cd aisafe
mvn test -pl aisafe.core
```

### Run a Specific Test Class

```sh
cd aisafe
mvn test -pl aisafe.core -Dtest=YourTestClassName
```

### Run a Specific Test Method

```sh
cd aisafe
mvn test -pl aisafe.core -Dtest=YourTestClassName#yourMethodName
```

### Clean and Run All Tests

```sh
cd aisafe
mvn clean test
```

---

## 5. How to Run

### Prerequisites

- Java 21 installed
- GCC (required only for the C simulation engine)
- JavaFX SDK (required only for the JavaFX dashboard — `run-javafx.sh`)
- PostgreSQL (optional — only if using Option B below)

---

### Quick Start — Unified Launcher

The easiest way to run the application is via the unified launcher, which starts H2 automatically and presents a menu:

**Linux / macOS (H2):**
```sh
cd aisafe
./run-all.sh
```

**Windows (H2):**
```bat
cd aisafe
run-all.bat
```

For PostgreSQL, use the postgres-specific launcher instead (see Option B below for the required environment variables or `.env` file):

**Linux / macOS (PostgreSQL):**
```sh
cd aisafe
./run-all-postgres.sh
```

**Windows (PostgreSQL):**
```bat
cd aisafe
run-all-postgres.bat
```

Both launchers present an interactive menu to choose Backoffice, Weather Client, or Air Transport.

---

### Option A — In-Memory H2 Database (No setup required)

#### 1. Start the H2 Server

**Linux / macOS:**
```sh
cd aisafe
./start-h2-server.sh
```

**Windows:**
```bat
cd aisafe
start-h2-server.bat
```

#### 2. Run Bootstrap (first time only)

Populates the database with initial seed data. Only needed once per fresh database.

**Linux / macOS:**
```sh
cd aisafe
./run-bootstrap.sh
```

**Windows:**
```bat
cd aisafe
run-bootstrap.bat
```

> **Note:** Re-running bootstrap on an existing database may produce duplicate data.

#### 3. Connect to H2 (optional — for inspection)

**Linux / macOS:**
```bash
java -cp $(find ~/.m2/repository/com/h2database/h2 -name "h2-*.jar" | head -1) \
  org.h2.tools.Shell -url "jdbc:h2:tcp://localhost/~/aisafe-dev" -user sa
```

**Windows (PowerShell):**
```powershell
$jar = (Get-ChildItem "$env:USERPROFILE\.m2\repository\com\h2database\h2" -Recurse -Filter "h2-*.jar")[0].FullName
java -cp $jar org.h2.tools.Shell -url "jdbc:h2:tcp://localhost/~/aisafe-dev" -user sa
```

---

### Option B — PostgreSQL Database

#### 1. Install and Configure PostgreSQL

1. Install **PostgreSQL 18**.
2. Open **pgAdmin 4** (or DataGrip).
3. Create a new database named **`aisafe`**.
4. Test the connection to confirm it is working.

See [docs/database-setup.md](docs/database-setup.md) for DEI-hosted cloud database connection details.

#### 2. Configure Environment Variables

Set the following variables before launching any application script. They must be in the **same terminal session**.

**Linux / macOS:**
```sh
export AISAFE_PERSISTENCE_UNIT=aisafe-pu-postgres
export AISAFE_DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect
export AISAFE_DB_DRIVER=org.postgresql.Driver
export AISAFE_DB_URL=jdbc:postgresql://localhost:5432/aisafe
export AISAFE_DB_USERNAME=postgres
export AISAFE_DB_PASSWORD=postgres
```

**Windows — CMD:**
```cmd
set AISAFE_PERSISTENCE_UNIT=aisafe-pu-postgres
set AISAFE_DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect
set AISAFE_DB_DRIVER=org.postgresql.Driver
set AISAFE_DB_URL=jdbc:postgresql://localhost:5432/aisafe
set AISAFE_DB_USERNAME=postgres
set AISAFE_DB_PASSWORD=postgres
```

**Windows — PowerShell:**
```powershell
$env:AISAFE_PERSISTENCE_UNIT="aisafe-pu-postgres"
$env:AISAFE_DB_DIALECT="org.hibernate.dialect.PostgreSQLDialect"
$env:AISAFE_DB_DRIVER="org.postgresql.Driver"
$env:AISAFE_DB_URL="jdbc:postgresql://localhost:5432/aisafe"
$env:AISAFE_DB_USERNAME="postgres"
$env:AISAFE_DB_PASSWORD="postgres"
```

**Alternative — `.env` file (for `run-all-postgres`):**

Create `aisafe/.env` with the key-value pairs above. `run-all-postgres.sh` / `run-all-postgres.bat` will load it automatically.

---

### Launching Application Components

After choosing your database option and (if needed) running bootstrap, launch the desired components.

#### JavaFX Dashboard

A graphical dashboard for real-time visualization of simulation and weather data. Requires a JavaFX-compatible JDK or SDK on the path.

**Linux / macOS:**
```sh
cd aisafe
./run-javafx.sh
```

**Windows:**
```bat
cd aisafe
run-javafx.bat
```

---

#### Backoffice Console

**Linux / macOS:**
```sh
cd aisafe
./run-backoffice.sh
```

**Windows:**
```bat
cd aisafe
run-backoffice.bat
```

---

#### Air Transport Console

**Local mode** (standalone — no TCP):

**Linux / macOS:**
```sh
cd aisafe
./run-airtransport.sh
```

**Windows:**
```bat
cd aisafe
run-airtransport.bat
```

**Server mode** (required for Pilot remote access — starts a TCP server on port 9002 by default):

**Linux / macOS:**
```sh
cd aisafe
./run-airtransport-server.sh         # default port 9002
./run-airtransport-server.sh 9003    # custom port
```

**Windows:**
```bat
cd aisafe
run-airtransport-server.bat
run-airtransport-server.bat 9003
```

**Client mode** (connects to a running Air Transport server):

**Linux / macOS:**
```sh
cd aisafe
./run-airtransport-client.sh                    # connects to localhost:9002
./run-airtransport-client.sh myhost 9003        # custom host and port
```

**Windows:**
```bat
cd aisafe
run-airtransport-client.bat
run-airtransport-client.bat myhost 9003
```

---

#### Weather Server and Weather Client

The weather server (integrated with the Air Transport / Air Control backend) must be running before the client connects.

**Start the Weather TCP Server (Linux / macOS):**
```sh
cd aisafe
./run-weatherserver.sh
```

**Windows:**
```bat
cd aisafe
run-weatherserver.bat
```

**Start the Weather Client (Linux / macOS):**
```sh
cd aisafe
./run-weatherclient.sh
```

**Windows:**
```bat
cd aisafe
run-weatherclient.bat
```

---

#### Pilot Remote Access (US086)

The Pilot server wraps the Air Transport domain and exposes it over TCP for remote pilot clients.

**Start the Pilot TCP Server (Linux / macOS):**
```sh
cd aisafe
./run-pilot-server.sh
```

**Windows:**
```bat
cd aisafe
run-pilot-server.bat
```

**Start the Pilot Client (Linux / macOS):**
```sh
cd aisafe
./run-pilot-client.sh                     # connects to localhost:9001
./run-pilot-client.sh myhost 9001         # custom host and port
```

**Windows:**
```bat
cd aisafe
run-pilot-client.bat
run-pilot-client.bat myhost 9001
```

---

#### Remote Accesses Logging Server (US090)

Records remote access events and exposes an HTTP status page (optionally HTTPS).

**Linux / macOS:**
```sh
cd aisafe
./run-logging-server.sh
```

**Windows:**
```bat
cd aisafe
run-logging-server.bat
```

Optional environment variables (all have defaults):

| Variable | Default | Description |
|---|---|---|
| `AISAFE_LOGGING_PORT` | `9999` | TCP port for log event ingestion |
| `AISAFE_LOGGING_HTTP_PORT` | `8080` | HTTP/HTTPS port for the status page |
| `AISAFE_LOGGING_DIR` | `logs/` | Directory where log files are written |
| `AISAFE_LOGGING_TLS_KEYSTORE` | *(unset)* | Path to a PKCS12 keystore — enables HTTPS |
| `AISAFE_LOGGING_TLS_KEYSTORE_PASSWORD` | `Teste1234!` | Keystore password |

**Generate a self-signed certificate (optional — enables HTTPS):**

**Linux / macOS:**
```sh
cd aisafe
./generate-logging-keystore.sh
```

**Windows:**
```bat
cd aisafe
generate-logging-keystore.bat
```

Then set `AISAFE_LOGGING_TLS_KEYSTORE` to the generated file path before starting the server.

---

#### C Simulation Engine

The simulation engine is a standalone C program. It reads `simulation_settings.json` and all active flight plans from the database, runs the physics simulation, and produces `simulation_output.txt` and a `simulation_report_*.json`.

**Build and run the simulation only (Linux — requires GCC):**
```sh
cd aisafe/aisafe-simulation
make run
```

**Run simulation and persist the result to the database:**
```sh
cd aisafe
./run-simulation-and-record.sh
```

This script:
1. Compiles the C engine (if needed) via `make run`.
2. Runs the simulation and selects all flights automatically.
3. Invokes the Java recorder, which reads the newest `simulation_report_*.json` and persists it to the database.

> **Note:** The simulation engine runs on Linux only (requires POSIX shared memory, named semaphores, and `fork()`).

---

### Recommended Startup Order

For a full system session (all components active):

1. Start H2 (`./start-h2-server.sh`) **or** export PostgreSQL env vars
2. Run Bootstrap (`./run-bootstrap.sh`) — first time only
3. Start the **Logging Server** (`./run-logging-server.sh`) — optional
4. Start the **Weather Server** (`./run-weatherserver.sh`)
5. Start the **Air Transport Server** (`./run-airtransport-server.sh`)
6. Start the **Pilot TCP Server** (`./run-pilot-server.sh`)
7. Start the **Backoffice Console** (`./run-backoffice.sh`)
8. Connect a **Weather Client** (`./run-weatherclient.sh`)
9. Connect a **Pilot Client** (`./run-pilot-client.sh`)
10. Run the **Simulation** (`./run-simulation-and-record.sh`) after flight plans are ready
11. Open the **JavaFX Dashboard** (`./run-javafx.sh`) to observe results
