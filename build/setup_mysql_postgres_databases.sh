#!/usr/bin/env bash

set -e

# import setup functions
. build/setup_db_scripts.sh

# run setup scripts for local databases
time setup_mysql $MYSQL_SCRIPT 127.0.0.1
time setup_postgres $POSTGRES_SCRIPT 127.0.0.1

echo "Postgres and MySQL Databases are ready!"