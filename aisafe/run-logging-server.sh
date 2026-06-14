#!/usr/bin/env bash

# Run the Remote Accesses Logging Server (US090)
# Configuration via environment variables:
#   AISAFE_LOGGING_PORT       (default: 9999)
#   AISAFE_LOGGING_HTTP_PORT  (default: 8080)
#   AISAFE_LOGGING_DIR        (default: logs/)
#   AISAFE_LOGGING_TLS_KEYSTORE           (optional, enables HTTPS — see generate-logging-keystore.sh)
#   AISAFE_LOGGING_TLS_KEYSTORE_PASSWORD  (default: Teste1234!)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_CP="$SCRIPT_DIR/aisafe.logging/target/aisafe.logging-1.0.0-SNAPSHOT.jar:$SCRIPT_DIR/aisafe.logging/target/dependency/*"

JAVA_OPTS=()
[ -n "$AISAFE_LOGGING_PORT"      ] && JAVA_OPTS+=("-Daisafe.logging.port=$AISAFE_LOGGING_PORT")
[ -n "$AISAFE_LOGGING_HTTP_PORT" ] && JAVA_OPTS+=("-Daisafe.logging.http.port=$AISAFE_LOGGING_HTTP_PORT")
[ -n "$AISAFE_LOGGING_DIR"       ] && JAVA_OPTS+=("-Daisafe.logging.dir=$AISAFE_LOGGING_DIR")
[ -n "$AISAFE_LOGGING_TLS_KEYSTORE"          ] && JAVA_OPTS+=("-Daisafe.logging.tls.keystore=$AISAFE_LOGGING_TLS_KEYSTORE")
[ -n "$AISAFE_LOGGING_TLS_KEYSTORE_PASSWORD" ] && JAVA_OPTS+=("-Daisafe.logging.tls.keystore.password=$AISAFE_LOGGING_TLS_KEYSTORE_PASSWORD")

echo "Starting Remote Accesses Logging Server..."
java -cp "$BASE_CP" "${JAVA_OPTS[@]}" logging.RemoteAccessesLoggingServer
