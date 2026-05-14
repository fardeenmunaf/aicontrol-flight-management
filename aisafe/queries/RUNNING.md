# Running AISafe (macOS/Linux + Windows)

This project supports two persistence modes:

- In-memory (H2) for development/testing
- PostgreSQL for production-like usage

These scripts assume you already built the project and copied dependencies.

## Build (all OS)

Run once after changing code or dependencies:

```bash
./build-all.sh
```

```bat
build-all.bat
```

## In-memory (H2)

Use the bootstrap only in H2/dev.

### macOS/Linux

```bash
./start-h2-server.sh
./run-bootstrap.sh
./run-backoffice.sh
```

### Windows (PowerShell or CMD)

```bat
start-h2-server.bat
run-bootstrap.bat
run-backoffice.bat
```

## PostgreSQL (remote DB)

Do not run the bootstrap here unless you explicitly want to seed data.

### macOS/Linux

```bash
export AISAFE_PERSISTENCE_UNIT=aisafe-pu-postgres
export AISAFE_DB_URL=jdbc:postgresql://localhost:5432/aisafe
export AISAFE_DB_USERNAME=postgres
export AISAFE_DB_PASSWORD=postgres
./run-backoffice.sh
```

### Windows (CMD)

```bat
set AISAFE_PERSISTENCE_UNIT=aisafe-pu-postgres
set AISAFE_DB_URL=jdbc:postgresql://localhost:5432/aisafe
set AISAFE_DB_USERNAME=postgres
set AISAFE_DB_PASSWORD=postgres
run-backoffice.bat
```

### Windows (PowerShell)

```powershell
$env:AISAFE_PERSISTENCE_UNIT="aisafe-pu-postgres"
$env:AISAFE_DB_URL="jdbc:postgresql://localhost:5432/aisafe"
$env:AISAFE_DB_USERNAME="postgres"
$env:AISAFE_DB_PASSWORD="postgres"
.\run-backoffice.bat
```

## Classpath notes

The run scripts rely on jars in `target/` and dependencies in `target/dependency/`.
If you see `ClassNotFoundException`, rerun the build step above to regenerate the jars and dependencies.

### Manual classpath (Windows)

If the `.bat` scripts still fail, run these directly from the repo root:

```bat
set BASE_CP=aisafe.app.backoffice.console\target\aisafe.app.backoffice.console-1.0.0-SNAPSHOT.jar;aisafe.app.backoffice.console\target\dependency\*
java -cp %BASE_CP% backoffice.BackofficeApp
```

```bat
set BASE_CP=aisafe.app.bootstrap\target\aisafe.app.bootstrap-1.0.0-SNAPSHOT.jar;aisafe.app.bootstrap\target\dependency\*
java -cp %BASE_CP% bootstrap.AISafeBootstrap
```

### Manual classpath (macOS/Linux)

```bash
export BASE_CP=aisafe.app.backoffice.console/target/aisafe.app.backoffice.console-1.0.0-SNAPSHOT.jar:aisafe.app.backoffice.console/target/dependency/*
java -cp "$BASE_CP" backoffice.BackofficeApp
```

```bash
export BASE_CP=aisafe.app.bootstrap/target/aisafe.app.bootstrap-1.0.0-SNAPSHOT.jar:aisafe.app.bootstrap/target/dependency/*
java -cp "$BASE_CP" bootstrap.AISafeBootstrap
```
