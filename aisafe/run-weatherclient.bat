REM Copy dependencies if they don't exist
if not exist aisafe.app.weather.client\target\dependency (
    echo Copying dependencies...
    mvn -q dependency:copy-dependencies -DoutputDirectory=aisafe.app.weather.client\target\dependency -f aisafe.app.weather.client\pom.xml
)

REM set the class path
SET BASE_CP=aisafe.app.weather.client\target\aisafe.app.weather.client-1.0.0-SNAPSHOT.jar;aisafe.app.weather.client\target\dependency\*;

REM Optional database configuration via environment variables.
SET JAVA_OPTS=
IF NOT "%AISAFE_PERSISTENCE_UNIT%"=="" SET JAVA_OPTS=%JAVA_OPTS% -Daisafe.persistence.unit=%AISAFE_PERSISTENCE_UNIT%
IF NOT "%AISAFE_DB_DIALECT%"=="" SET JAVA_OPTS=%JAVA_OPTS% -Daisafe.db.dialect=%AISAFE_DB_DIALECT%
IF NOT "%AISAFE_DB_DRIVER%"=="" SET JAVA_OPTS=%JAVA_OPTS% -Daisafe.db.driver=%AISAFE_DB_DRIVER%
IF NOT "%AISAFE_DB_URL%"=="" SET JAVA_OPTS=%JAVA_OPTS% -Daisafe.db.url=%AISAFE_DB_URL%
IF NOT "%AISAFE_DB_USERNAME%"=="" SET JAVA_OPTS=%JAVA_OPTS% -Daisafe.db.username=%AISAFE_DB_USERNAME%
IF NOT "%AISAFE_DB_PASSWORD%"=="" SET JAVA_OPTS=%JAVA_OPTS% -Daisafe.db.password=%AISAFE_DB_PASSWORD%

REM call the java VM, e.g,
java -cp %BASE_CP% %JAVA_OPTS% weather.WeatherClientApp
