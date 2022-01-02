#!/usr/bin/env bash

export SQLITE_SCRIPT=quill-jdbc/src/test/resources/sql/sqlite-schema.sql
export MYSQL_SCRIPT=quill-sql/src/test/sql/mysql-schema.sql
export POSTGRES_SCRIPT=quill-sql/src/test/sql/postgres-schema.sql
export SQL_SERVER_SCRIPT=quill-sql/src/test/sql/sqlserver-schema.sql
export ORACLE_SCRIPT=quill-sql/src/test/sql/oracle-schema.sql
export CASSANDRA_SCRIPT=quill-cassandra/src/test/cql/cassandra-schema.cql


function get_host() {
    if [ -z "$1" ]; then
        echo "127.0.0.1"
    else
        echo "$1"
    fi
}
# usage: setup_x <script>

function setup_sqlite() {
    # DB File in quill-jdbc
    echo "Creating sqlite DB File"
    DB_FILE=quill-jdbc/quill_test.db
    echo "Removing Previous sqlite DB File (if any)"
    rm -f $DB_FILE
    echo "Creating sqlite DB File"
    echo "(with the $1 script)"
    sqlite3 $DB_FILE < $1
    echo "Setting permissions on sqlite DB File"
    chmod a+rw $DB_FILE

   # DB File in quill-jdbc-monix
   DB_FILE=quill-jdbc-monix/quill_test.db
   rm -f $DB_FILE
   sqlite3 $DB_FILE < $1
   chmod a+rw $DB_FILE

    echo "Sqlite ready!"
}

function setup_mysql() {
    port=$3
    password=''
    if [ -z "$port" ]; then
        echo "MySQL Port not defined. Setting to default: 3306  "
        port="3306"
    else
        echo "MySQL Port specified as $port"
    fi

    connection=$2
    MYSQL_ROOT_PASSWORD=root

    echo "Waiting for MySql"
    # If --protocol not set, --port is silently ignored so need to have it
    until mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root -e "select 1" &> /dev/null; do
        echo "Tapping MySQL Connection, this may show an error> mysql --protocol=tcp --host=$connection --password='$MYSQL_ROOT_PASSWORD' --port=$port -u root -e 'select 1'"
        mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root -e "select 1" || true
        sleep 5;
    done
    echo "Connected to MySql"

    echo "**Verifying MySQL Connection> mysql --protocol=tcp --host=$connection --password='...' --port=$port -u root -e 'select 1'"
    mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root -e "select 1"

    echo "MySql: Create codegen_test"
    mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root -e "CREATE DATABASE codegen_test;"
    echo "MySql: Create quill_test"
    mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root -e "CREATE DATABASE quill_test;"
    echo "MySql: Write Schema to quill_test"
    mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root quill_test < $1
    echo "MySql: Create finagle user"
    mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root -e "CREATE USER 'finagle'@'%' IDENTIFIED BY 'finagle';"
    echo "MySql: Grant finagle user"
    mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root -e "GRANT ALL PRIVILEGES ON * . * TO 'finagle'@'%';"
    echo "MySql: Flush the grant"
    mysql --protocol=tcp --host=$connection --password="$MYSQL_ROOT_PASSWORD" --port=$port -u root -e "FLUSH PRIVILEGES;"
}

function setup_postgres() {
    port=$3
    if [ -z "$port" ]; then
        echo "Postgres Port not defined. Setting to default: 5432"
        port="5432"
    else
        echo "Postgres Port specified as $port"
    fi
    echo "Waiting for Postgres"
    until psql --host $2 --port $port --username postgres -c "select 1" &> /dev/null; do
        echo "## Tapping Postgres Connection> psql --host $2 --port $port --username postgres -c 'select 1'"
        psql --host $2 --port $port --username postgres -c "select 1" || true
        sleep 5;
    done
    echo "Connected to Postgres"

    echo "Postgres: Create codegen_test"
    psql --host $2 --port $port -U postgres -c "CREATE DATABASE codegen_test"
    echo "Postgres: Create quill_test"
    psql --host $2 --port $port -U postgres -c "CREATE DATABASE quill_test"
    echo "Postgres: Write Schema to quill_test"
    psql --host $2 --port $port -U postgres -d quill_test -a -q -f $1
}

 function setup_cassandra() {
     host=$(get_host $2)
     echo "Waiting for Cassandra"
     until cqlsh $2 -e "describe cluster" &> /dev/null; do
         sleep 5;
     done
     echo "Connected to Cassandra"

     cqlsh $2 -f $1
 }

function setup_sqlserver() {
    host=$(get_host $2)
    echo "Waiting for SqlServer"
    until /opt/mssql-tools/bin/sqlcmd -S $2 -U SA -P "QuillRocks!" -Q "select 1" &> /dev/null; do
        sleep 5;
    done
    echo "Connected to SqlServer"

    /opt/mssql-tools/bin/sqlcmd -S $2 -U SA -P "QuillRocks!" -Q "CREATE DATABASE codegen_test"
    /opt/mssql-tools/bin/sqlcmd -S $2 -U SA -P "QuillRocks!" -Q "CREATE DATABASE alpha"
    /opt/mssql-tools/bin/sqlcmd -S $2 -U SA -P "QuillRocks!" -Q "CREATE DATABASE bravo"
    /opt/mssql-tools/bin/sqlcmd -S $2 -U SA -P "QuillRocks!" -Q "CREATE DATABASE quill_test"
    /opt/mssql-tools/bin/sqlcmd -S $2 -U SA -P "QuillRocks!" -d quill_test -i $1
}

# Do a simple necat poll to make sure the oracle database is ready.
# All internal database creation and schema setup scripts are handled
# by the container and docker-compose steps.

function setup_oracle() {
    while ! nc -z $2 1521; do
        echo "Waiting for Oracle"
        sleep 2;
    done;
    sleep 2;

    echo "Running Oracle Setup Script"
    java -cp '/sqlline/sqlline.jar:/sqlline/ojdbc.jar' 'sqlline.SqlLine' \
      -u 'jdbc:oracle:thin:@oracle:1521:xe' \
      -n quill_test -p 'QuillRocks!' \
      -f "$ORACLE_SCRIPT" \
      --showWarnings=false

    echo "Connected to Oracle"
    sleep 2
}

function send_script() {
  echo "Send Script Args: 1: $1 - 2 $2 - 3: $3"
  docker cp $2 "$(docker-compose ps -q $1)":/$3
}

export -f setup_sqlite
export -f setup_mysql
export -f setup_postgres
export -f setup_cassandra
export -f setup_sqlserver
export -f setup_oracle
export -f send_script