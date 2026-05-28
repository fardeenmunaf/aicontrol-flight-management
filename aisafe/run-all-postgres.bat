@echo off
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "ENV_FILE=%SCRIPT_DIR%.env"

REM Load .env if present (KEY=VALUE, lines starting with # are ignored)
if exist "%ENV_FILE%" (
  for /f "usebackq eol=# tokens=1* delims==" %%A in ("%ENV_FILE%") do (
    if not "%%A"=="" set "%%A=%%B"
  )
)

REM Validate required settings
set "MISSING="
for %%V in (AISAFE_PERSISTENCE_UNIT AISAFE_DB_DIALECT AISAFE_DB_DRIVER AISAFE_DB_URL AISAFE_DB_USERNAME AISAFE_DB_PASSWORD) do (
  if "!%%V!"=="" set "MISSING=!MISSING! %%V"
)
if not "!MISSING!"=="" (
  echo Missing required environment variables:!MISSING!
  echo Create a .env file in %SCRIPT_DIR% or export the variables before running.
  exit /b 1
)

REM Optional dry-run to verify configuration loading
if /i "%AISAFE_DRY_RUN%"=="1" (
  echo AISAFE_PERSISTENCE_UNIT=%AISAFE_PERSISTENCE_UNIT%
  echo AISAFE_DB_DIALECT=%AISAFE_DB_DIALECT%
  echo AISAFE_DB_DRIVER=%AISAFE_DB_DRIVER%
  echo AISAFE_DB_URL=%AISAFE_DB_URL%
  echo AISAFE_DB_USERNAME=%AISAFE_DB_USERNAME%
  echo AISAFE_DB_PASSWORD=***
  exit /b 0
)

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
call "%~dp0run-backoffice.bat"
pause
goto menu

:weather
echo Running Weather Client...
call "%~dp0run-weatherclient.bat"
pause
goto menu

:airtransport
echo Running Air Transport...
call "%~dp0run-airtransport.bat"
pause
goto menu