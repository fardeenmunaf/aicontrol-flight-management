@echo off
echo Running Backoffice Console...

mvn -pl aisafe.app.backoffice.console exec:java -Dexec.mainClass="eapli.app.backoffice.console.presentation.backoffice.BackofficeApp"

pause