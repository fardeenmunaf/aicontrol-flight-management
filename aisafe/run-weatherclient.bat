REM set the class path,
REM assumes the build was executed with maven copy-dependencies
SET BASE_CP=aisafe.app.weather.client\target\aisafe.app.weather.client-1.0.0-SNAPSHOT.jar;aisafe.app.weather.client\target\dependency\*;

REM call the java VM, e.g, 
java -cp %BASE_CP% weather.WeatherClientApp