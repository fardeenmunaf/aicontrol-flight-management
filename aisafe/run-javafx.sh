#!/usr/bin/env bash
set -e

# Start the JavaFX app
cd "$(dirname "$0")/aisafe.app.javafx"
mvn javafx:run
