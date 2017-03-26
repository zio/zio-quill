#!/bin/sh

echo "Waiting for Sql Server"
until sqlcmd -S sqlserver -U SA -P 'QuillRocks!' -Q "SELECT 1" &> /dev/null
do
  printf "."
  sleep 1
done
echo "\nSql Server ready"

sqlcmd -S sqlserver -U SA -P "QuillRocks!" -Q "CREATE DATABASE quill_test"
sqlcmd -S sqlserver -U SA -P "QuillRocks!" -d quill_test -i quill-sql/src/test/sql/sqlserver-schema.sql
