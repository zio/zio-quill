#!/bin/bash

# Copy the jdbc jar from the container to local
docker cp "$(docker-compose ps -q oracle)":/u01/app/oracle-product/12.1.0/xe/jdbc/lib/ojdbc7.jar ./

mvn install:install-file \
  -Dfile=ojdbc7.jar \
  -DgroupId='com.oracle.jdbc' \
  -DartifactId=ojdbc7 \
  -Dversion=12.1.0.2 \
  -Dpackaging=jar

rm ojdbc7.jar
