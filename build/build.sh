#!/usr/bin/env bash

set -e

function show_mem() {
    free -m | awk 'NR==2{printf "Memory Usage: %s/%sMB (%.2f%%)\n", $3,$2,$3*100/$2 }'
}
export -f show_mem

export POSTGRES_HOST=127.0.0.1
export POSTGRES_PORT=5432

export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306

export SQL_SERVER_HOST=127.0.0.1
export SQL_SERVER_PORT=11433

export CASSANDRA_HOST=127.0.0.1
export CASSANDRA_PORT=19042

export ORIENTDB_HOST=127.0.0.1
export ORIENTDB_PORT=12424

export SBT_ARGS="-Dquill.macro.log=false -Xms512m -Xmx1536m -Xss2m -XX:ReservedCodeCacheSize=256m -XX:+TieredCompilation -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC ++$TRAVIS_SCALA_VERSION"

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

echo "Running tests"
time sbt $SBT_ARGS checkUnformattedFiles test tut doc

show_mem

time docker-compose down
# for 2.11 publish coverage
if [[ $TRAVIS_SCALA_VERSION == 2.11* ]]; then

    echo "Coverage"
    time sbt $SBT_ARGS coverageReport coverageAggregate
    pip install --user codecov && codecov
fi

show_mem

sleep 10