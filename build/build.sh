#!/usr/bin/env bash

set -e

function show_mem() {
    free -m | awk 'NR==2{printf "Memory Usage: %s/%sMB (%.2f%%)\n", $3,$2,$3*100/$2 }'

    echo "===== System Memory Stats Start ====="
    ps -eo size,%mem,cmd --sort=-size | head -10 | awk '{ hr=$1/1024 ; printf("%13.2fMb ",hr); print($2 " " $0) }'
    sleep 2
    echo "===== System Memory Stats End ====="

    echo "===== Docker Stats Start ====="
    docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" --no-stream
    sleep 4
    echo "===== Docker Stats End ====="
}
export -f show_mem

export POSTGRES_HOST=127.0.0.1
export POSTGRES_PORT=5432

export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306

export SQL_SERVER_HOST=127.0.0.1
export SQL_SERVER_PORT=11433

export ORACLE_HOST=127.0.0.1
export ORACLE_PORT=11521

export CASSANDRA_HOST=127.0.0.1
export CASSANDRA_PORT=19042

export ORIENTDB_HOST=127.0.0.1
export ORIENTDB_PORT=12424

export SBT_ARGS="-Doracle=true -Dquill.macro.log=false -Xms1024m -Xmx3g -Xss5m -XX:ReservedCodeCacheSize=256m -XX:+TieredCompilation -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC ++$TRAVIS_SCALA_VERSION"

if [[ $TRAVIS_SCALA_VERSION == 2.11* ]]; then
    export SBT_ARGS="$SBT_ARGS coverage"
fi

show_mem

# Start sbt compilation and database setup in parallel
sbt clean $SBT_ARGS scalariformFormat test:scalariformFormat quill-coreJVM/test:compile & COMPILE=$!
./build/setup_travis.sh & SETUP=$!

# Wait on database setup. If it has failed then kill compilation process and exit with error
wait $SETUP

if [[ "$?" != "0" ]]; then
   echo "Database setup failed"
   sleep 10
   kill -9 $COMPILE
   exit 1
fi
echo "Setup is finished, waiting for the compilation of core module"

wait $COMPILE
if [[ "$?" != "0" ]]; then
   echo "Compilation of core module has failed"
   sleep 10
   exit 1
fi
show_mem

time sbt $SBT_ARGS checkUnformattedFiles test tut doc

show_mem

echo "Tests completed. Shutting down"

time docker-compose down
# for 2.11 publish coverage
if [[ $TRAVIS_SCALA_VERSION == 2.11* ]]; then

    echo "Coverage"
    time sbt $SBT_ARGS coverageReport coverageAggregate
    pip install --user codecov && codecov
fi

show_mem

sleep 10