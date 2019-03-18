#!/usr/bin/env bash

set -e

# import setup functions
. /app/build/setup_db_scripts.sh


time setup_mysql $MYSQL_SCRIPT mysql
time setup_postgres $POSTGRES_SCRIPT postgres
time setup_cassandra $CASSANDRA_SCRIPT cassandra
time setup_sqlserver $SQL_SERVER_SCRIPT sqlserver
time setup_oracle $ORACLE_SCRIPT oracle

# TODO Move this back up to the top. This is failing for now but want mysql to succeed
time setup_sqlite $SQLITE_SCRIPT

echo "Databases are ready!"