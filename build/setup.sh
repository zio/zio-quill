#!/bin/sh

DB_FILE=quill_test.db

rm $DB_FILE

echo "Waiting for Sqlite"
until sqlite3 $DB_FILE "SELECT 1" &> /dev/null
do
  printf "."
  sleep 1
done
echo -e "\nSqlite ready"

sqlite3 $DB_FILE < quill-jdbc/src/test/resources/sql/sqlite-schema.sql

echo "Waiting for Mysql"
until mysql -u root -proot -h mysql -e "SELECT 1" &> /dev/null
do
  printf "."
  sleep 1
done
echo -e "\nMysql ready"

mysql -u root -proot -h mysql -e "ALTER USER 'root'@'%' IDENTIFIED BY ''"
mysql -u root -h mysql quill_test < quill-sql/src/test/sql/mysql-schema.sql
mysql -u root -h mysql -e "CREATE USER 'finagle'@'%' IDENTIFIED BY 'finagle';"
mysql -u root -h mysql -e "GRANT ALL PRIVILEGES ON * . * TO 'finagle'@'%';"
mysql -u root -h mysql -e "FLUSH PRIVILEGES;"

echo "Waiting for Postgres"
until psql -h postgres -U postgres -c "SELECT 1" &> /dev/null
do
  printf "."
  sleep 1
done
echo -e "\nPostgres ready"

psql -h postgres -U postgres -c "CREATE DATABASE quill_test"
psql -h postgres -U postgres -d quill_test -a -f quill-sql/src/test/sql/postgres-schema.sql

echo "Waiting for Cassandra"
if ! nc -z cassandra 9042; then
    while ! nc -z cassandra 9042; do
       printf "."
       sleep 1
    done
    echo -e "\nCassandra ready"
fi

echo "CREATE KEYSPACE quill_test WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};" > /tmp/create-keyspace.cql
cqlsh cassandra -f /tmp/create-keyspace.cql
cqlsh cassandra -k quill_test -f quill-cassandra/src/test/cql/cassandra-schema.cql