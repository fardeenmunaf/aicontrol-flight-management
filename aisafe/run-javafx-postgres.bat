@echo off
setlocal EnableDelayedExpansion

for /f "usebackq eol=# tokens=1* delims==" %%A in ("%~dp0.env") do (
    if not "%%A"=="" set "%%A=%%B"
)

cd /d "%~dp0aisafe.app.javafx"
mvn javafx:run
pause