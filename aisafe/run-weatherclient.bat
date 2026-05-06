REM Copy dependencies if they don't exist
if not exist aisafe.app.weather.client\target\dependency (
    echo Copying dependencies...
    mvn -q dependency:copy-dependencies -DoutputDirectory=aisafe.app.weather.client\target\dependency -f aisafe.app.weather.client\pom.xml
)

REM set the class path
SET BASE_CP=aisafe.app.weather.client\target\aisafe.app.weather.client-1.0.0-SNAPSHOT.jar;aisafe.app.weather.client\target\dependency\*;

REM call the java VM, e.g, 
java -cp %BASE_CP% weather.WeatherClientApp