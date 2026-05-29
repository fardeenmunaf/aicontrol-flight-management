@echo off
echo Building AISafe Project and gathering dependencies...
mvn clean install dependency:copy-dependencies -DskipTests
echo Build complete!
