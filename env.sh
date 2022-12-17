#!/usr/bin/env bash
export CASSANDRA_HOST=127.0.0.1
export CASSANDRA_PORT=19042
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=13306
export POSTGRES_HOST=127.0.0.1
export POSTGRES_PORT=15432
export SQL_SERVER_HOST=127.0.0.1
export SQL_SERVER_PORT=11433
export ORIENTDB_HOST=127.0.0.1
export ORIENTDB_PORT=12424
export ORACLE_HOST=127.0.0.1
export ORACLE_PORT=11521

export JVM_OPTS="-Dquill.macro.log=false -Xms1024m -Xmx3g -Xss5m -XX:ReservedCodeCacheSize=256m -XX:+TieredCompilation -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC"

