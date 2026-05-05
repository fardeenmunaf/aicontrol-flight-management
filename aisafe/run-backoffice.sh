#!/usr/bin/env bash

#REM set the class path,
#REM assumes the build was executed with maven copy-dependencies
export BASE_CP=aisafe.app.backoffice.console/target/aisafe.app.backoffice.console-1.0.0-SNAPSHOT.jar:aisafe.app.backoffice.console/target/dependency/*;

#REM call the java VM, e.g,
java -cp $BASE_CP backoffice.BackofficeApp
