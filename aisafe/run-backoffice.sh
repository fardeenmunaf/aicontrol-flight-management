#!/usr/bin/env bash

#REM set the class path,
#REM assumes the build was executed with maven copy-dependencies
export BASE_CP=aisafe.app.backoffice.console/target/aisafe.app.backoffice.console-1.0.0-SNAPSHOT.jar:aisafe.app.backoffice.console/target/dependency/*;

# Optional database configuration via environment variables.
JAVA_OPTS=()
if [ -n "$AISAFE_PERSISTENCE_UNIT" ]; then
  JAVA_OPTS+=("-Daisafe.persistence.unit=$AISAFE_PERSISTENCE_UNIT")
fi
if [ -n "$AISAFE_DB_DIALECT" ]; then
  JAVA_OPTS+=("-Daisafe.db.dialect=$AISAFE_DB_DIALECT")
fi
if [ -n "$AISAFE_DB_DRIVER" ]; then
  JAVA_OPTS+=("-Daisafe.db.driver=$AISAFE_DB_DRIVER")
fi
if [ -n "$AISAFE_DB_URL" ]; then
  JAVA_OPTS+=("-Daisafe.db.url=$AISAFE_DB_URL")
fi
if [ -n "$AISAFE_DB_USERNAME" ]; then
  JAVA_OPTS+=("-Daisafe.db.username=$AISAFE_DB_USERNAME")
fi
if [ -n "$AISAFE_DB_PASSWORD" ]; then
  JAVA_OPTS+=("-Daisafe.db.password=$AISAFE_DB_PASSWORD")
fi

#REM call the java VM, e.g,
java -cp $BASE_CP "${JAVA_OPTS[@]}" backoffice.BackofficeApp
