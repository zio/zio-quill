#!/usr/bin/env bash

set -e

time docker-compose up -d cassandra orientdb

# import setup functions
. build/setup_db_scripts.sh

# setup cassandra in docker
send_script cassandra $CASSANDRA_SCRIPT cassandra-schema.cql
send_script cassandra ./build/setup_db_scripts.sh setup_db_scripts.sh
time docker-compose exec -T cassandra bash -c ". setup_db_scripts.sh && setup_cassandra cassandra-schema.cql 127.0.0.1"

echo "Databases are ready!"