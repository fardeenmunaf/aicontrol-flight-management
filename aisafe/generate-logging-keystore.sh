#!/usr/bin/env bash

# Generates a self-signed certificate + private key for the Remote Accesses
# Logging Server's HTTPS status page (US90/US91), as covered in RCOMP TP11
# (SSL/TLS network programming, self-signed public key certificates).
#
# Usage: ./generate-logging-keystore.sh [hostname] [keystore-file] [password]

HOSTNAME="${1:-localhost}"
KEYSTORE="${2:-logging-keystore.p12}"
PASSWORD="${3:-changeit}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
case "$KEYSTORE" in
    /*) KEYSTORE_PATH="$KEYSTORE" ;;
    *)  KEYSTORE_PATH="$SCRIPT_DIR/$KEYSTORE" ;;
esac

if [ -f "$KEYSTORE_PATH" ]; then
    echo "Keystore already exists at $KEYSTORE_PATH — remove it first to regenerate."
    exit 1
fi

keytool -genkeypair \
    -alias aisafe-logging \
    -keyalg RSA -keysize 2048 \
    -validity 3650 \
    -dname "CN=$HOSTNAME, OU=AISafe, O=ISEP, C=PT" \
    -storetype PKCS12 \
    -keystore "$KEYSTORE_PATH" \
    -storepass "$PASSWORD" || exit 1

echo "Self-signed certificate created at $KEYSTORE_PATH"
echo "Run the logging server with:"
echo "  AISAFE_LOGGING_TLS_KEYSTORE=$KEYSTORE_PATH AISAFE_LOGGING_TLS_KEYSTORE_PASSWORD=$PASSWORD ./run-logging-server.sh"
