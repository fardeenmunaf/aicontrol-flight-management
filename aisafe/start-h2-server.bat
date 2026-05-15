@echo off
setlocal

set H2_VERSION=2.2.224
set H2_JAR=%USERPROFILE%\.m2\repository\com\h2database\h2\%H2_VERSION%\h2-%H2_VERSION%.jar

if not exist "%H2_JAR%" (
    echo H2 JAR not found at: %H2_JAR%
    pause
    exit /b 1
)

echo Starting H2 Server...

start "H2 Server" cmd /k java -cp "%H2_JAR%" org.h2.tools.Server -tcp -tcpPort 9092 -ifNotExists

echo H2 server started in another window.
exit /b 0
