@echo off
echo Cleaning and Rebuilding AISafe Project...
mvn clean package dependency:copy-dependencies -DskipTests
echo Rebuild complete!
