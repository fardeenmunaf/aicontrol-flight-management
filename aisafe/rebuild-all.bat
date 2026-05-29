@echo off
echo Cleaning and Rebuilding AISafe Project...
mvn clean install dependency:copy-dependencies -DskipTests
echo Rebuild complete!
