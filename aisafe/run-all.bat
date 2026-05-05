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
call run-backoffice.bat
pause
goto menu

:weather
echo Running Weather Client...
call run-weatherclient.bat
pause
goto menu

:aircontrol
echo Running Air Control...
echo (Not implemented yet)
pause
goto menu

:airtransport
echo Running Air Transport...
echo (Not implemented yet)
pause
goto menu