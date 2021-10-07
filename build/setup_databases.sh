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
time setup_sqlite $SQLITE_SCRIPT 127.0.0.1
echo "### Running Setup for mysql ###"
time setup_mysql $MYSQL_SCRIPT 127.0.0.1 13306
echo "### Running Setup for postgres ###"
time setup_postgres $POSTGRES_SCRIPT 127.0.0.1 15432

echo "### Running Setup for sqlserver ###"
# setup sqlserver in docker
send_script sqlserver $SQL_SERVER_SCRIPT sqlserver-schema.sql
send_script sqlserver ./build/setup_db_scripts.sh setup_db_scripts.sh
time docker-compose exec -T sqlserver bash -c ". setup_db_scripts.sh && setup_sqlserver sqlserver-schema.sql 127.0.0.1"

echo "### Starting to Wait for Oracle ###"
while ! nc -z 127.0.0.1 11521; do
    echo "Waiting for Oracle"
    sleep 2;
done;
sleep 2;

echo "Oracle Setup Complete"

echo "Databases are ready!"