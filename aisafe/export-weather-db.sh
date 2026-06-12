#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

BASE_CP="$SCRIPT_DIR/aisafe.app.javafx/target/aisafe.app.javafx-1.0.0-SNAPSHOT.jar:$SCRIPT_DIR/aisafe.app.javafx/target/dependency/*"

JAVA_OPTS=()
if [ -n "$AISAFE_PERSISTENCE_UNIT" ]; then
  JAVA_OPTS+=("-Daisafe.persistence.unit=$AISAFE_PERSISTENCE_UNIT")
fi
if [ -n "$AISAFE_DB_DIALECT" ]; then
  JAVA_OPTS+=("-Daisafe.db.dialect=$AISAFE_DB_DIALECT")
fi
if [ -n "$AISAFE_DB_DRIVER" ]; then
  JAVA_OPTS+=("-Daisafe.db.driver=$AISAFE_DB_DRIVER")
fi
if [ -n "$AISAFE_DB_URL" ]; then
  JAVA_OPTS+=("-Daisafe.db.url=$AISAFE_DB_URL")
fi
if [ -n "$AISAFE_DB_USERNAME" ]; then
  JAVA_OPTS+=("-Daisafe.db.username=$AISAFE_DB_USERNAME")
fi
if [ -n "$AISAFE_DB_PASSWORD" ]; then
  JAVA_OPTS+=("-Daisafe.db.password=$AISAFE_DB_PASSWORD")
fi

cd "$SCRIPT_DIR"
java -cp "$BASE_CP" "${JAVA_OPTS[@]}" javafxui.ExportWeatherDataRunner "$@"
