# Project AIControl

## 1. Description of the Project

Project AIControl is a prototype software solution designed for flight management and air traffic control simulation. The system aims to manage complex aviation operations, validate flight plans, and simulate air traffic to ensure safety and prevent mid-air collisions.

This integrated system is composed of several core modules:
* **Backoffice Configuration:** Management of aviation assets, including aircraft models, engine models, airports, air transport companies, and flight control areas.
* **Flight Management & DSL:** Creation, parsing, and semantic validation of flight plans using a custom Domain Specific Language (Core Flight DSL) processed via ANTLR.
* **Air Traffic Simulation:** A highly scalable, multi-process, and multi-threaded simulation engine developed in C. It tracks aircraft trajectories, applies aerodynamic physics (Lift and Drag), and detects safety violations in real-time.
* **Weather Integration:** A service that incorporates environmental data (such as wind speed and direction) into the flight simulations to model realistic conditions.
* **Network & Remote Access:** TCP/UDP-based remote access and logging services, including a real-time web visualization dashboard updated dynamically.

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

This will compile and package all project modules (Backoffice, DSL, Simulation, etc.).

---

## 4. How to Execute Tests

### Prerequisites

- Java 21 installed
- Project built at least once (see **Section 3 — How to Build**)

***

### Run All Tests (from project root)

**Linux / macOS:**
```sh
cd sem4pi2526-sem4pi2526_2dk5
mvn test
```

**Windows:**
```bat
cd sem4pi2526-sem4pi2526_2dk5
mvn test
```

***

### Run Tests for a Specific Module

To run tests only for the `aisafe.core` module:

**Linux / macOS / Windows:**
```sh
cd sem4pi2526-sem4pi2526_2dk5
mvn test -pl aisafe/aisafe.core
```

***

### Run a Specific Test Class

```sh
mvn test -pl aisafe/aisafe.core -Dtest=YourTestClassName
```

### Run a Specific Test Method

```sh
mvn test -pl aisafe/aisafe.core -Dtest=YourTestClassName#yourMethodName
```

***

### Clean and Run All Tests

```sh
cd sem4pi2526-sem4pi2526_2dk5
mvn clean test
```

***


## 5. How to Run

### Prerequisites

- Java 21 installed
- (Optional) PostgreSQL 18 installed, if using a relational database

---

### Option A — In-Memory H2 Database (No setup required)

If you want to run the application using the embedded H2 in-memory database, start the H2 server first:

**Windows:**
```bat
start-h2-server.bat
```

**Linux / macOS:**
```sh
./start-h2-server.sh
```

Then proceed to launch the application modules as described below (without setting any environment variables).

To connect to the H2 database via the shell (for debugging or inspection):

**Windows (PowerShell):**
```powershell
$jar = (Get-ChildItem "$env:USERPROFILE\.m2\repository\com\h2database\h2" -Recurse -Filter "h2-*.jar")[0].FullName; java -cp $jar org.h2.tools.Shell -url "jdbc:h2:tcp://localhost/~/aisafe-dev" -user sa
```

**Linux / macOS:**
```bash
java -cp $(find ~/.m2/repository/com/h2database/h2 -name "h2-*.jar" | head -1) org.h2.tools.Shell -url "jdbc:h2:tcp://localhost/~/aisafe-dev" -user sa
```

---

### Option B — PostgreSQL Database

#### 1. Install and Configure PostgreSQL

1. Install **PostgreSQL 18**.
2. Open **pgAdmin 4**.
3. Create a new database named **`aisafe`**:
   - Right-click on *Databases* → *Create* → *Database...*
   - Set the name to `aisafe` and save.
4. Test the connection to confirm it is working.

#### 2. Set Environment Variables

Before launching the application, configure the following environment variables in your terminal session:

**Windows — PowerShell:**
```powershell
$env:AISAFE_PERSISTENCE_UNIT="aisafe-pu-postgres"
$env:AISAFE_DB_URL="jdbc:postgresql://localhost:5432/aisafe"
$env:AISAFE_DB_USERNAME="postgres"
$env:AISAFE_DB_PASSWORD="postgres"
```

**Windows — CMD:**
```cmd
set AISAFE_PERSISTENCE_UNIT=aisafe-pu-postgres
set AISAFE_DB_URL=jdbc:postgresql://localhost:5432/aisafe
set AISAFE_DB_USERNAME=postgres
set AISAFE_DB_PASSWORD=postgres
```

**Linux / macOS:**
```sh
export AISAFE_PERSISTENCE_UNIT=aisafe-pu-postgres
export AISAFE_DB_URL=jdbc:postgresql://localhost:5432/aisafe
export AISAFE_DB_USERNAME=postgres
export AISAFE_DB_PASSWORD=postgres
```

> **Note:** These variables must be set in the **same terminal session** where you will run the scripts below. If you open a new terminal, you will need to set them again.

---

### Bootstrap (Initial Data Load)

Before launching the application for the first time, run the bootstrap to populate the database with initial data:

**Linux / macOS:**
```sh
./aisafe/run-bootstrap.sh
```

> **Note:** Only needs to be run once per fresh database. Re-running it on an existing database may result in duplicate data.

---

### Launching the Application

After choosing your database option (H2 or PostgreSQL), setting the environment variables (if using PostgreSQL), and running bootstrap, launch the desired application module:

**Backoffice — Linux / macOS:**
```sh
./aisafe/run-backoffice.sh
```

**Air Transport — Linux / macOS:**
```sh
./aisafe/run-airtransport.sh
```



