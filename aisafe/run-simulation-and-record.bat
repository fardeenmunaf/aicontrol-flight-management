@echo off
setlocal enabledelayedexpansion

REM ---------------------------------------------------------------------------
REM run-simulation-and-record.bat
REM
REM 1. Compiles and runs the C simulation engine via `make run` (selects all
REM    flights automatically via stdin).
REM 2. Once the simulation finishes, invokes the Java recorder which reads
REM    the newest simulation_report_*.json and persists it to the database.
REM
REM Must be executed from the aisafe\ root directory.
REM ---------------------------------------------------------------------------

set "SCRIPT_DIR=%~dp0"
REM Remove trailing backslash
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

set "SIM_DIR=%SCRIPT_DIR%\aisafe-simulation"
set "BASE_CP=%SCRIPT_DIR%\aisafe.app.airtransport.console\target\aisafe.app.airtransport.console-1.0.0-SNAPSHOT.jar;%SCRIPT_DIR%\aisafe.app.airtransport.console\target\dependency\*"

REM ---------- Optional DB overrides (same env-vars as run-airtransport.bat) ---
set "JAVA_OPTS="
if defined AISAFE_PERSISTENCE_UNIT set "JAVA_OPTS=%JAVA_OPTS% -Daisafe.persistence.unit=%AISAFE_PERSISTENCE_UNIT%"
if defined AISAFE_DB_DIALECT       set "JAVA_OPTS=%JAVA_OPTS% -Daisafe.db.dialect=%AISAFE_DB_DIALECT%"
if defined AISAFE_DB_DRIVER        set "JAVA_OPTS=%JAVA_OPTS% -Daisafe.db.driver=%AISAFE_DB_DRIVER%"
if defined AISAFE_DB_URL           set "JAVA_OPTS=%JAVA_OPTS% -Daisafe.db.url=%AISAFE_DB_URL%"
if defined AISAFE_DB_USERNAME      set "JAVA_OPTS=%JAVA_OPTS% -Daisafe.db.username=%AISAFE_DB_USERNAME%"
if defined AISAFE_DB_PASSWORD      set "JAVA_OPTS=%JAVA_OPTS% -Daisafe.db.password=%AISAFE_DB_PASSWORD%"

REM ---------- Step 1: run the C simulation --------------------------------
echo === [1/2] Running simulation engine ===
cd /d "%SIM_DIR%"

REM Pipe "a" then empty line to the engine (select all flights)
(echo a & echo.) | make --no-print-directory run
if errorlevel 1 (
    echo ERROR: Simulation engine failed.
    exit /b 1
)

REM ---------- Step 2: record the result in the DB -------------------------
echo.
echo === [2/2] Recording simulation result to database ===
cd /d "%SCRIPT_DIR%"
java -cp "%BASE_CP%" %JAVA_OPTS% airtransport.AirtransportApp --record
if errorlevel 1 (
    echo ERROR: Java recorder failed.
    exit /b 1
)

echo.
echo Done.
endlocal
