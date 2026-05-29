#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
set -a; . "$SCRIPT_DIR/.env"; set +a
cd "$SCRIPT_DIR/aisafe.app.javafx"
mvn javafx:run