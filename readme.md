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

This will compile and package all project modules (Backoffice, Bootstrap, DSL, Simulation, etc.).

---

## 4. How to Execute Tests

*To Do*

## 5. How to Run

### Prerequisites

- Java 17+ installed
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

### Launching the Application

After choosing your database option (H2 or PostgreSQL) and setting the environment variables (if using PostgreSQL), launch the application modules in order:

**Windows:**
```bat
run-bootstrap.bat
run-backoffice.bat
```

**Linux / macOS:**
```sh
./run-bootstrap.sh
./run-backoffice.sh
```

## 6. How to Install/Deploy into Another Machine (or Virtual Machine)

*To Do*

## 7. How to Generate PlantUML Diagrams

To generate plantuml diagrams for documentation execute the script (for the moment, only for linux/unix/macos):

    ./generate-plantuml-diagrams.sh


