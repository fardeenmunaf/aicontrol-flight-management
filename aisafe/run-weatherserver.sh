#!/bin/bash

echo "Starting Weather TCP Server..."

# Ensure we are in the directory where the script is located
cd "$(dirname "$0")"

# Compile the module
mvn compile -pl aisafe.app.weather.client

# Execute the main class
mvn exec:java -pl aisafe.app.weather.client -Dexec.mainClass="weather.server.WeatherTcpServer" -Dexec.cleanupDaemonThreads=false
