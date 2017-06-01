#!/usr/bin/env bash

DB_FILE=quill_test.db

rm $DB_FILE

function waitFor() {
  echo "Waiting for $1"
  i=20
  until eval $2 &> /dev/null
  do
    printf ".$i"
    if [[ "$i" -eq 0 ]]; then
      printf "timeout!"
      exit 1
    fi
    i=$((i-1))
    sleep 1s
  done
  echo -e "\n$1 ready"
}

sqlite3 $DB_FILE < quill-jdbc/src/test/resources/sql/sqlite-schema.sql


waitFor 'Mysql' 'mysql -u root -proot -h mysql -e "SELECT 1"'

mysql -u root -proot -h mysql quill_test < quill-sql/src/test/sql/mysql-schema.sql
mysql -u root -proot -h mysql -e "CREATE USER 'finagle'@'%' IDENTIFIED BY 'finagle';"
mysql -u root -proot -h mysql -e "GRANT ALL PRIVILEGES ON * . * TO 'finagle'@'%';"
mysql -u root -proot -h mysql -e "FLUSH PRIVILEGES;"


waitFor 'Postgres' 'psql -h postgres -U postgres -c "SELECT 1"'

psql -h postgres -U postgres -c "CREATE DATABASE quill_test"
psql -h postgres -U postgres -d quill_test -a -f quill-sql/src/test/sql/postgres-schema.sql


waitFor 'Cassandra' 'nc -z cassandra 9042'

echo "CREATE KEYSPACE quill_test WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};" > /tmp/create-keyspace.cql
cqlsh cassandra -f /tmp/create-keyspace.cql
cqlsh cassandra -k quill_test -f quill-cassandra/src/test/cql/cassandra-schema.cql


waitFor 'SqlServer' 'sqlcmd -S sqlserver -U SA -P 'QuillRocks!' -Q "SELECT 1"'

sqlcmd -S sqlserver -U SA -P "QuillRocks!" -Q "CREATE DATABASE quill_test"
sqlcmd -S sqlserver -U SA -P "QuillRocks!" -d quill_test -i quill-sql/src/test/sql/sqlserver-schema.sql
