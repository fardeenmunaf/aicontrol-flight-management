@echo off
echo Running Weather Client...

mvn -pl aisafe.app.weather.client exec:java -Dexec.mainClass="eapli.app.weather.client.WeatherClientApp"

pause