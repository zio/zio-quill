#!/usr/bin/env bash

set -e

time docker-compose up -d cassandra sqlserver orientdb

# import setup functions
. build/setup_db_scripts.sh

# run setup scripts for local databases
time setup_sqlite $SQLITE_SCRIPT 127.0.0.1
time setup_mysql $MYSQL_SCRIPT 127.0.0.1
time setup_postgres $POSTGRES_SCRIPT 127.0.0.1


function send_script() {
  docker cp $2 "$(docker-compose ps -q $1)":/$3
}

# setup sqlserver in docker
send_script sqlserver $SQL_SERVER_SCRIPT sqlserver-schema.sql
send_script sqlserver ./build/setup_db_scripts.sh setup_db_scripts.sh
time docker-compose exec -T sqlserver bash -c ". setup_db_scripts.sh && setup_sqlserver sqlserver-schema.sql 127.0.0.1"

# setup cassandra in docker
send_script cassandra $CASSANDRA_SCRIPT cassandra-schema.cql
send_script cassandra ./build/setup_db_scripts.sh setup_db_scripts.sh
time docker-compose exec -T cassandra bash -c ". setup_db_scripts.sh && setup_cassandra cassandra-schema.cql 127.0.0.1"

echo "Databases are ready!"