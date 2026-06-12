#!/bin/bash

echo "Starting Pilot TCP Server (US086)..."

cd "$(dirname "$0")"

mvn install -pl aisafe.app.pilot.client -am -DskipTests -q

mvn exec:java -pl aisafe.app.pilot.client \
    -Dexec.mainClass="pilot.server.PilotTcpServer" \
    -Dexec.cleanupDaemonThreads=false
