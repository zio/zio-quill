#!/usr/bin/env bash

echo "Waiting for Sql Server"
until /opt/mssql-tools/bin/sqlcmd -S sqlserver -U SA -P 'QuillRocks!' -Q "SELECT 1" &> /dev/null
do
  printf "."
  sleep 1
done
echo -e "\nSql Server ready"

/opt/mssql-tools/bin/sqlcmd -S sqlserver -U SA -P "QuillRocks!" -Q "DROP DATABASE IF EXISTS quill_test"
/opt/mssql-tools/bin/sqlcmd -S sqlserver -U SA -P "QuillRocks!" -Q "CREATE DATABASE quill_test"
/opt/mssql-tools/bin/sqlcmd -S sqlserver -U SA -P "QuillRocks!" -d quill_test -i /app/quill-sql/src/test/sql/sqlserver-schema.sql