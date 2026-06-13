#!/usr/bin/env bash

#REM set the class path,
#REM assumes the build was executed with maven copy-dependencies
export BASE_CP=aisafe.app.airtransport.console/target/aisafe.app.airtransport.console-1.0.0-SNAPSHOT.jar:aisafe.app.airtransport.console/target/dependency/*;

# Server host (default localhost, override with first argument) and port
# (default 9002, override with second argument).
HOST="${1:-localhost}"
PORT="${2:-9002}"

# Optional database configuration via environment variables.
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

#REM call the java VM, e.g,
java -cp $BASE_CP "${JAVA_OPTS[@]}" airtransport.AirtransportApp --remote "$HOST" "$PORT"
