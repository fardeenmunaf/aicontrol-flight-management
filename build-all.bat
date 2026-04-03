@echo off
REM Build only the root project (ignoring submodules)
mvn clean install -N
