@echo off
echo Starting Remote Accesses Logging Server...
cd /d %~dp0
call mvn compile -pl aisafe.logging
call mvn exec:java -pl aisafe.logging -Dexec.mainClass="logging.RemoteAccessesLoggingServer"
pause