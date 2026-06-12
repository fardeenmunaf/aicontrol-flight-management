#!/usr/bin/env bash

# Pilot Remote Access Client (US086)
# Assumes the build was executed with: mvn package -pl aisafe.app.pilot.client -am

cd "$(dirname "$0")"

export BASE_CP=aisafe.app.pilot.client/target/aisafe.app.pilot.client-1.0.0-SNAPSHOT.jar:aisafe.app.pilot.client/target/dependency/*

# Server host/port can be overridden via environment variables or args
HOST=${1:-${AISAFE_PILOT_HOST:-localhost}}
PORT=${2:-${AISAFE_PILOT_PORT:-9001}}

JAVA_OPTS=()
if [ -n "$AISAFE_PERSISTENCE_UNIT" ]; then
  JAVA_OPTS+=("-Daisafe.persistence.unit=$AISAFE_PERSISTENCE_UNIT")
fi

java -cp "$BASE_CP" "${JAVA_OPTS[@]}" pilot.PilotClientApp "$HOST" "$PORT"
