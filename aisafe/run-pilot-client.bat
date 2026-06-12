@echo off
REM Pilot Remote Access Client (US086)
REM Assumes the build was executed with: mvn package -pl aisafe.app.pilot.client -am

cd /d %~dp0

REM Copy dependencies if they don't exist
if not exist aisafe.app.pilot.client\target\dependency (
    echo Copying dependencies...
    mvn -q dependency:copy-dependencies -DoutputDirectory=aisafe.app.pilot.client\target\dependency -f aisafe.app.pilot.client\pom.xml
)

REM Set class path
SET BASE_CP=aisafe.app.pilot.client\target\aisafe.app.pilot.client-1.0.0-SNAPSHOT.jar;aisafe.app.pilot.client\target\dependency\*

REM Server host/port (defaults: localhost:9001)
SET HOST=%AISAFE_PILOT_HOST%
IF "%HOST%"=="" SET HOST=localhost
SET PORT=%AISAFE_PILOT_PORT%
IF "%PORT%"=="" SET PORT=9001

SET JAVA_OPTS=
IF NOT "%AISAFE_PERSISTENCE_UNIT%"=="" SET JAVA_OPTS=%JAVA_OPTS% -Daisafe.persistence.unit=%AISAFE_PERSISTENCE_UNIT%

java -cp %BASE_CP% %JAVA_OPTS% pilot.PilotClientApp %HOST% %PORT%
pause
