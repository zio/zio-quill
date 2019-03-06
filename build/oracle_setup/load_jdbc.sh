#!/bin/bash

# Copy the jdbc jar from the container to local
docker cp "$(docker-compose ps -q oracle)":/opt/oracle/product/18c/dbhomeXE/jdbc/lib/ojdbc8.jar ./

mvn install:install-file \
  -Dfile=ojdbc8.jar \
  -DgroupId='com.oracle.jdbc' \
  -DartifactId=ojdbc8 \
  -Dversion=18.3.0.0.0 \
  -Dpackaging=jar

rm ojdbc8.jar
