#!/bin/bash

mysql -u root -proot -h mysql -e "ALTER USER 'root'@'%' IDENTIFIED BY ''"
mysql -u root -h mysql quill_test < quill-sql/src/test/sql/mysql-schema.sql
mysql -u root -h mysql -e "CREATE USER 'finagle'@'%' IDENTIFIED BY 'finagle';"
mysql -u root -h mysql -e "GRANT ALL PRIVILEGES ON * . * TO 'finagle'@'%';"
mysql -u root -h mysql -e "FLUSH PRIVILEGES;"

createdb -h postgres quill_test -U postgres
psql -h postgres -U postgres -d quill_test -a -f quill-sql/src/test/sql/postgres-schema.sql

echo "CREATE KEYSPACE quill_test WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};" > /tmp/create-keyspace.cql
cqlsh cassandra -f /tmp/create-keyspace.cql
cqlsh cassandra -k quill_test -f quill-cassandra/src/test/cql/cassandra-schema.cql