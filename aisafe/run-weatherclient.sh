#!/usr/bin/env bash

#REM set the class path,
#REM assumes the build was executed with maven copy-dependencies
export BASE_CP=aisafe.app.weatherclient/target/aisafe.app.weatherclient-1.0.0-SNAPSHOT.jar:aisafe.app.weatherclient/target/dependency/*;

#REM call the java VM, e.g,
java -cp $BASE_CP weatherclient.WeatherClientApp
