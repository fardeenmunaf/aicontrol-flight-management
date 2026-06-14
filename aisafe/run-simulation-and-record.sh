#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# run-simulation-and-record.sh
#
# 1. Compiles and runs the C simulation engine via `make run` (selects all
#    flights automatically via stdin).
# 2. Once the simulation finishes, invokes the Java recorder which reads
#    the newest simulation_report_*.json and persists it to the database.
#
# Must be executed from the aisafe/ root directory.
# ---------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SIM_DIR="$SCRIPT_DIR/aisafe-simulation"
BASE_CP="$SCRIPT_DIR/aisafe.app.airtransport.console/target/aisafe.app.airtransport.console-1.0.0-SNAPSHOT.jar:$SCRIPT_DIR/aisafe.app.airtransport.console/target/dependency/*"

# ---------- Optional DB overrides (same env-vars as run-airtransport.sh) ---
JAVA_OPTS=()
[ -n "${AISAFE_PERSISTENCE_UNIT:-}" ] && JAVA_OPTS+=("-Daisafe.persistence.unit=$AISAFE_PERSISTENCE_UNIT")
[ -n "${AISAFE_DB_DIALECT:-}"       ] && JAVA_OPTS+=("-Daisafe.db.dialect=$AISAFE_DB_DIALECT")
[ -n "${AISAFE_DB_DRIVER:-}"        ] && JAVA_OPTS+=("-Daisafe.db.driver=$AISAFE_DB_DRIVER")
[ -n "${AISAFE_DB_URL:-}"           ] && JAVA_OPTS+=("-Daisafe.db.url=$AISAFE_DB_URL")
[ -n "${AISAFE_DB_USERNAME:-}"      ] && JAVA_OPTS+=("-Daisafe.db.username=$AISAFE_DB_USERNAME")
[ -n "${AISAFE_DB_PASSWORD:-}"      ] && JAVA_OPTS+=("-Daisafe.db.password=$AISAFE_DB_PASSWORD")

# ---------- Step 1: run the C simulation --------------------------------
echo "=== [1/2] Running simulation engine ==="
cd "$SIM_DIR"
# `make run` builds if needed, then pipes "a\n\n" to the engine (select all flights).
printf 'a\n\n' | make --no-print-directory run

# ---------- Step 2: record the result in the DB -------------------------
echo ""
echo "=== [2/2] Recording simulation result to database ==="
cd "$SCRIPT_DIR"
java -cp "$BASE_CP" "${JAVA_OPTS[@]}" airtransport.AirtransportApp --record

echo ""
echo "Done."
