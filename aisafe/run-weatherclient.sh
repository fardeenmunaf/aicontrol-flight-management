#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

#REM set the class path,
#REM assumes the build was executed with maven copy-dependencies
export BASE_CP="$SCRIPT_DIR/aisafe.app.weather.client/target/aisafe.app.weather.client-1.0.0-SNAPSHOT.jar:$SCRIPT_DIR/aisafe.app.weather.client/target/dependency/*"

#REM call the java VM, e.g,
java -cp "$BASE_CP" weather.WeatherClientApp
