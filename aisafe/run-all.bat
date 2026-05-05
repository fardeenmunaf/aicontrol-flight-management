@echo off
:menu
cls
echo ================================
echo        AISAFE RUN MENU
echo ================================
echo.
echo 1 - Backoffice Console
echo 2 - Weather Client
echo 3 - Air Control Console
echo 4 - Air Transport Console
echo 0 - Exit
echo.
set /p choice=Select an option:

if "%choice%"=="1" goto backoffice
if "%choice%"=="2" goto weather
if "%choice%"=="3" goto aircontrol
if "%choice%"=="4" goto airtransport
if "%choice%"=="0" exit

goto menu

:backoffice
echo Running Backoffice...
mvn -pl aisafe.app.backoffice.console exec:java -Dexec.mainClass="eapli.app.backoffice.console.presentation.backoffice.BackofficeApp"
pause
goto menu

:weather
echo Running Weather Client...
mvn -pl aisafe.app.weather.client exec:java -Dexec.mainClass="eapli.app.weather.client.WeatherClientApp"
pause
goto menu

:aircontrol
echo Running Air Control...
mvn -pl aisafe.app.aircontrol.console exec:java -Dexec.mainClass="eapli.app.aircontrol.console.AirControlApp"
pause
goto menu

:airtransport
echo Running Air Transport...
mvn -pl aisafe.app.airtransport.console exec:java -Dexec.mainClass="eapli.app.airtransport.console.AirTransportApp"
pause
goto menu