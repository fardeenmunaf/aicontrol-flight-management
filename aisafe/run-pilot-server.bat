@echo off
echo Starting Pilot TCP Server (US086)...
cd /d %~dp0
call mvn install -pl aisafe.app.pilot.client -am -DskipTests -q
call mvn exec:java -pl aisafe.app.pilot.client -Dexec.mainClass="pilot.server.PilotTcpServer" -Dexec.cleanupDaemonThreads=false
pause
