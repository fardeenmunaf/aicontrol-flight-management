#!/bin/bash

# Script to start H2 Server in TCP mode (in-memory)
# This server allows multiple applications to access the same in-memory database

H2_VERSION="2.2.224"
H2_JAR="$HOME/.m2/repository/com/h2database/h2/$H2_VERSION/h2-$H2_VERSION.jar"

if [ ! -f "$H2_JAR" ]; then
    echo " H2 JAR not found at: $H2_JAR"
    echo "   Make sure you ran 'mvn clean install' first"
    exit 1
fi

echo " Starting H2 Server in TCP mode (in-memory)..."
echo "   URL: jdbc:h2:tcp://localhost:9092/mem:aisafe"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

java -cp "$H2_JAR" org.h2.tools.Server -tcp -tcpPort 9092 -ifNotExists

