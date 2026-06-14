@echo off
REM Generates a self-signed certificate + private key for the Remote Accesses
REM Logging Server's HTTPS status page (US90/US91), as covered in RCOMP TP11
REM (SSL/TLS network programming, self-signed public key certificates).
REM
REM Usage: generate-logging-keystore.bat [hostname] [keystore-file] [password]

setlocal
set HOSTNAME=%1
if "%HOSTNAME%"=="" set HOSTNAME=localhost

set KEYSTORE=%2
if "%KEYSTORE%"=="" set KEYSTORE=logging-keystore.p12

set PASSWORD=%3
if "%PASSWORD%"=="" set PASSWORD=Teste1234!

cd /d %~dp0

if exist "%KEYSTORE%" (
    echo Keystore already exists at %KEYSTORE% — remove it first to regenerate.
    exit /b 1
)

keytool -genkeypair ^
    -alias aisafe-logging ^
    -keyalg RSA -keysize 2048 ^
    -validity 3650 ^
    -dname "CN=%HOSTNAME%, OU=AISafe, O=ISEP, C=PT" ^
    -storetype PKCS12 ^
    -keystore "%KEYSTORE%" ^
    -storepass "%PASSWORD%" || exit /b 1

echo Self-signed certificate created at %KEYSTORE%
echo Run the logging server with:
echo   set AISAFE_LOGGING_TLS_KEYSTORE=%CD%\%KEYSTORE%
echo   set AISAFE_LOGGING_TLS_KEYSTORE_PASSWORD=%PASSWORD%
echo   run-logging-server.bat
