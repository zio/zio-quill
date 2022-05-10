#!/usr/bin/env bash

set -e

# import setup functions
. /app/build/setup_db_scripts.sh


time setup_mysql mysql
time setup_postgres postgres
time setup_cassandra cassandra $CASSANDRA_SCRIPT
# SQL Server needs to be passed different script paths based on environment. Therefore it has a 2nd arg.
time setup_sqlserver sqlserver $SQL_SERVER_SCRIPT
time setup_oracle oracle

# TODO Move this back up to the top. This is failing for now but want mysql to succeed
time setup_sqlite

echo "Databases are ready!"