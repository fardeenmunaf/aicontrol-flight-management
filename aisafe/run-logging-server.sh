#!/usr/bin/env bash

# Run the Remote Accesses Logging Server (US090)
# Configuration via environment variables:
#   AISAFE_LOGGING_PORT  (default: 9999)
#   AISAFE_LOGGING_DIR   (default: logs/)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_CP="$SCRIPT_DIR/aisafe.logging/target/aisafe.logging-1.0.0-SNAPSHOT.jar:$SCRIPT_DIR/aisafe.logging/target/dependency/*"

JAVA_OPTS=()
[ -n "$AISAFE_LOGGING_PORT" ] && JAVA_OPTS+=("-Daisafe.logging.port=$AISAFE_LOGGING_PORT")
[ -n "$AISAFE_LOGGING_DIR"  ] && JAVA_OPTS+=("-Daisafe.logging.dir=$AISAFE_LOGGING_DIR")

echo "Starting Remote Accesses Logging Server..."
java -cp "$BASE_CP" "${JAVA_OPTS[@]}" logging.RemoteAccessesLoggingServer
