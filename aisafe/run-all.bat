@echo off
echo Running Bootstrap...
call run-bootstrap.bat
pause

:menu
cls
echo ================================
echo        AISAFE RUN MENU
echo ================================
echo.
echo 1 - Backoffice Console
echo 2 - Weather Client
echo 3 - Air Transport Console
echo 0 - Exit
echo.
set /p choice=Select an option:

if "%choice%"=="1" goto backoffice
if "%choice%"=="2" goto weather
if "%choice%"=="3" goto airtransport
if "%choice%"=="0" exit

goto menu

:backoffice
echo Running Backoffice...
call run-backoffice.bat
pause
goto menu

:weather
echo Running Weather Client...
call run-weatherclient.bat
pause
goto menu

:airtransport
echo Running Air Transport...
call run-airtransport.bat
pause
goto menu