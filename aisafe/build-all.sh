#!/usr/bin/env bash
echo "Building AISafe Project and gathering dependencies..."
mvn clean package dependency:copy-dependencies -DskipTests
echo "Build complete!"
