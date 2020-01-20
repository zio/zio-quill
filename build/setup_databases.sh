#!/usr/bin/env bash

set -e

time docker-compose up -d sqlserver oracle

# import setup functions
. build/setup_db_scripts.sh

# run setup scripts for local databases
time setup_sqlite $SQLITE_SCRIPT 127.0.0.1
time setup_mysql $MYSQL_SCRIPT 127.0.0.1
time setup_postgres $POSTGRES_SCRIPT 127.0.0.1

# setup sqlserver in docker
send_script sqlserver $SQL_SERVER_SCRIPT sqlserver-schema.sql
send_script sqlserver ./build/setup_db_scripts.sh setup_db_scripts.sh
time docker-compose exec -T sqlserver bash -c ". setup_db_scripts.sh && setup_sqlserver sqlserver-schema.sql 127.0.0.1"

until docker-compose exec -T oracle "/oracle_setup_local/external_check_script.sh" | grep "match_this_test_to_pass"; do
  docker-compose exec -T oracle "/oracle_setup_local/external_check_script.sh"
  echo "Waiting for Oracle"
  sleep 5;
done
sleep 5;
echo "Oracle Setup Complete"

echo "Databases are ready!"