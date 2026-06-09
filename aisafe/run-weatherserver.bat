@echo off
echo Starting Weather TCP Server...
cd /d %~dp0
call mvn compile -pl aisafe.app.weather.client
call mvn exec:java -pl aisafe.app.weather.client -Dexec.mainClass="weather.server.WeatherTcpServer" -Dexec.cleanupDaemonThreads=false
pause
