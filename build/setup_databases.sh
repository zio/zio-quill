#!/usr/bin/env bash

set -e

echo "### Bringing Down Any Docker Containers that May Be Running ###"
time docker-compose down --rmi all

echo "### Bringing Up sqlserver, oracle, postgres, mysql Images ###"
time docker-compose up -d sqlserver oracle postgres mysql
echo "### DONE Bringing Up sqlserver and oracle Images ###"

echo "### Checking Docker Images"
docker ps

# import setup functions
echo "### Sourcing DB Scripts ###"
. build/setup_db_scripts.sh

# run setup scripts for local databases
echo "### Running Setup for sqlite ###"
time setup_sqlite 127.0.0.1
echo "### Running Setup for mysql ###"
time setup_mysql 127.0.0.1 13306
echo "### Running Setup for postgres ###"
time setup_postgres 127.0.0.1 15432

echo "### Running Setup for sqlserver ###"
# setup sqlserver in docker
send_script sqlserver $SQL_SERVER_SCRIPT sqlserver-schema.sql
send_script sqlserver ./build/setup_db_scripts.sh setup_db_scripts.sh
time docker-compose exec -T sqlserver bash -c ". setup_db_scripts.sh && setup_sqlserver 127.0.0.1 sqlserver-schema.sql"

# Can't do absolute paths here so need to do relative
mkdir sqlline/
curl 'https://repo1.maven.org/maven2/sqlline/sqlline/1.12.0/sqlline-1.12.0-jar-with-dependencies.jar' -o 'sqlline/sqlline.jar'
curl 'https://repo1.maven.org/maven2/com/oracle/ojdbc/ojdbc8/19.3.0.0/ojdbc8-19.3.0.0.jar' -o 'sqlline/ojdbc.jar'

echo "### Starting to Wait for Oracle ###"
while ! nc -z 127.0.0.1 11521; do
    echo "Waiting for Oracle"
    sleep 2;
done;

echo "Running Oracle Setup Script"
java -cp 'sqlline/sqlline.jar:sqlline/ojdbc.jar' 'sqlline.SqlLine' \
  -u 'jdbc:oracle:thin:@localhost:11521:xe' \
  -n quill_test -p 'QuillRocks!' \
  -f "$ORACLE_SCRIPT" \
  --showWarnings=false

sleep 2;

echo "Oracle Setup Complete"

echo "Databases are ready!"