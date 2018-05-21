#!/usr/bin/env bash

set -e

# import setup functions
. /app/build/setup_db_scripts.sh

time setup_sqlite $SQLITE_SCRIPT
time setup_mysql $MYSQL_SCRIPT mysql
time setup_postgres $POSTGRES_SCRIPT postgres
time setup_cassandra $CASSANDRA_SCRIPT cassandra
time setup_sqlserver $SQL_SERVER_SCRIPT sqlserver

echo "Databases are ready!"