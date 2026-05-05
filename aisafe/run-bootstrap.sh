#!/usr/bin/env bash

#REM set the class path,
#REM assumes the build was executed with maven copy-dependencies
export BASE_CP=aisafe.app.bootstrap/target/aisafe.app.bootstrap-1.0.0-SNAPSHOT.jar:aisafe.app.bootstrap/target/dependency/*;

#REM call the java VM, e.g,
java -cp $BASE_CP bootstrap.AISafeBootstrap
