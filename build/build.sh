#!/usr/bin/env bash

set -e

export POSTGRES_HOST=127.0.0.1
export POSTGRES_PORT=15432

export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=13306

export SQL_SERVER_HOST=127.0.0.1
export SQL_SERVER_PORT=11433

export ORACLE_HOST=127.0.0.1
export ORACLE_PORT=11521

export CASSANDRA_HOST=127.0.0.1
export CASSANDRA_PORT=19042

export CASSANDRA_CONTACT_POINT_0=127.0.0.1:19042
export CASSANDRA_DC=datacenter1

export ORIENTDB_HOST=127.0.0.1
export ORIENTDB_PORT=12424

export JVM_OPTS="-Dquill.macro.log=false -Dquill.scala.version=$SCALA_VERSION -Xms1024m -Xmx3g -Xss5m -XX:ReservedCodeCacheSize=256m -XX:+TieredCompilation -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC"

modules=$1
echo "Start build modules: $modules"


function show_mem() {
    free -m | awk 'NR==2{printf "Memory Usage: %s/%sMB (%.2f%%)\n", $3,$2,$3*100/$2 }'
    mem_details
    free_details
    docker_stats
}
export -f show_mem

function mem_details() {
    echo "===== System Memory Stats Start ====="
    ps -eo size,rss,%mem,cmd --sort=-size | head -20 | awk '{ hr=$1/1024; rr=$2/1024; printf("%13.2fMb ",hr); printf("%13.2fMb ",rr); print(" " $0) }'
    sleep 2
    echo "===== System Memory Stats End ====="
}
export -f mem_details

function free_details() {
    echo "===== Free Memory Stats Start ====="
    free -h
    sleep 2
    echo "===== Free Memory Stats End ====="
}
export -f free_details

function docker_stats() {
    echo "===== Docker Stats Start ====="
    docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" --no-stream
    docker ps
    sleep 4
    echo "===== Docker Stats End ====="
}
export -f docker_stats

export SBT_ARGS="++$SCALA_VERSION"

if [[ $SCALA_VERSION == 2.12* ]]; then
    export SBT_ARGS="$SBT_ARGS coverage"
fi

function wait_for_databases() {
    show_mem

    #sbt scalariformFormat test:scalariformFormat
    #sbt checkUnformattedFiles

    # Start sbt compilation and database setup in parallel
    echo "build.sh =:> Base Compile in wait_for_databases"
    sbt -Dmodules=base $SBT_ARGS  test & COMPILE=$!
    ./build/setup_databases.sh & SETUP=$!

    # Wait on database setup. If it has failed then kill compilation process and exit with error
    wait $SETUP

    if [[ "$?" != "0" ]]; then
       echo "build.sh =:> Database setup failed"
       sleep 10
       kill -9 $COMPILE
       exit 1
    fi

    echo "build.sh =:> Database Setup is finished, waiting for the compilation of core module"

    wait $COMPILE
    if [[ "$?" != "0" ]]; then
       echo "build.sh =:> Compilation of core module has failed"
       sleep 10
       exit 1
    fi

    echo "build.sh =:> Database Setup is finished"
    show_mem
}

function wait_for_mysql_postgres() {
    show_mem

    #sbt scalariformFormat test:scalariformFormat
    #sbt checkUnformattedFiles

    # Start sbt compilation and database setup in parallel
    echo "build.sh =:> Base Compile in wait_for_mysql_postgres"
    sbt -Dmodules=base $SBT_ARGS  test & COMPILE=$!
    ./build/setup_mysql_postgres_databases.sh & SETUP=$!

    # Wait on database setup. If it has failed then kill compilation process and exit with error
    wait $SETUP

    if [[ "$?" != "0" ]]; then
       echo "build.sh =:> Database setup failed"
       sleep 10
       kill -9 $COMPILE
       exit 1
    fi
    echo "build.sh =:> Database Setup is finished, waiting for the compilation of core module"

    wait $COMPILE
    if [[ "$?" != "0" ]]; then
       echo "build.sh =:> Compilation of core module has failed"
       sleep 10
       exit 1
    fi

    echo "build.sh =:> Database Setup is finished"
    show_mem
}

function wait_for_bigdata() {
    show_mem

    sbt scalariformFormat test:scalariformFormat
    sbt checkUnformattedFiles
    sbt $SBT_ARGS quill-coreJVM/test:compile & COMPILE=$!
    ./build/setup_bigdata.sh & SETUP=$!

    wait $SETUP
    if [[ "$?" != "0" ]]; then
       echo "build.sh =:> BigData Database setup failed"
       sleep 10
       kill -9 $COMPILE
       exit 1
    fi

    wait $COMPILE
    if [[ "$?" != "0" ]]; then
       echo "build.sh =:> Compilation of core module has failed"
       sleep 10
       exit 1
    fi

    echo "build.sh =:> BigData Database Setup is finished"
    show_mem
}

function db_build() {
    echo "build.sh =:> DB Build Specified"
    wait_for_databases
    export JVM_OPTS="-Dquill.macro.log=false -Dquill.scala.version=$SCALA_VERSION -Xms4g -Xmx4g -Xss10m -XX:ReservedCodeCacheSize=256m -XX:+TieredCompilation -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC"
    echo "build.sh =:> Starting DB Build Primary"
    ./build/aware_run.sh "sbt -Dmodules=db $SBT_ARGS test"
}

function js_build() {
    echo "build.sh =:> JS Build Specified"
    show_mem
    export JVM_OPTS="-Dquill.macro.log=false -Dquill.scala.version=$SCALA_VERSION -Xms4g -Xmx4g -Xss10m -XX:ReservedCodeCacheSize=256m -XX:+TieredCompilation -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC"
    echo "build.sh =:> Starting JS Build Primary"
    sbt -Dmodules=js $SBT_ARGS test
}

function async_build() {
    echo "build.sh =:> Async Build Specified"
    wait_for_mysql_postgres
    echo "build.sh =:> Starting Async Build Primary"
    sbt -Dmodules=async $SBT_ARGS test
}

function codegen_build() {
    echo "build.sh =:> Codegen Build Specified"
    wait_for_databases
    echo "build.sh =:> Starting Codegen Build Primary"
    sbt -Dmodules=codegen $SBT_ARGS test
}

function bigdata_build() {
    echo "build.sh =:> BigData Build Specified"
    wait_for_bigdata
    echo "build.sh =:> Starting BigData Build Primary"
    sbt -Dmodules=bigdata $SBT_ARGS test
}

function full_build() {
    echo "build.sh =:> Full Build Specified"
    wait_for_databases
    wait_for_bigdata
    echo "build.sh =:> Starting Full Build Primary"
    sbt $SBT_ARGS test
}

if [[ (! -z "$DOCKER_USERNAME") && (! -z "$DOCKER_USERNAME") ]]; then
  echo "Logging into Docker via $DOCKER_USERNAME for $EVENT_TYPE"
  echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

  echo "Getting Per-Account Statistics for Docker Pull Limits"
  TOKEN=$(curl --user "$DOCKER_USERNAME:$DOCKER_PASSWORD" "https://auth.docker.io/token?service=registry.docker.io&scope=repository:ratelimitpreview/test:pull" | jq -r .token)
  curl --head -H "Authorization: Bearer $TOKEN" https://registry-1.docker.io/v2/ratelimitpreview/test/manifests/latest
else
  echo "Getting Anonymous-Account Statistics for Docker Pull Limits"
  TOKEN=$(curl "https://auth.docker.io/token?service=registry.docker.io&scope=repository:ratelimitpreview/test:pull" | jq -r .token)
  curl --head -H "Authorization: Bearer $TOKEN" https://registry-1.docker.io/v2/ratelimitpreview/test/manifests/latest

  echo "Not Logging into Docker for $EVENT_TYPE build, restarting Docker using GCR mirror"
fi

echo "Building cache:"
find ~/.cache/sbt-build/ -type d || true

if [[ $modules == "db" ]]; then
    echo "build.sh =:> Build Script: Doing Database Build"
    db_build
elif [[ $modules == "js" ]]; then
    echo "build.sh =:> Build Script: Doing JavaScript Build"
    js_build
elif [[ $modules == "finagle" ]]; then
    echo "build.sh =:> Build Script: Doing Finagle Database Build"
    finagle_build
elif [[ $modules == "async" ]]; then
    echo "build.sh =:> Build Script: Doing Async Database Build"
    async_build
elif [[ $modules == "codegen" ]]; then
    echo "build.sh =:> Build Script: Doing Code Generator Build"
    codegen_build
elif [[ $modules == "bigdata" ]]; then
    echo "build.sh =:> Build Script: Doing BigData Build"
    bigdata_build
else
    echo "build.sh =:> Build Script: Doing Full Build"
    full_build
fi

show_mem
echo "Tests completed. Shutting down"
time docker-compose down
# for 2.12 publish coverage
if [[ $SCALA_VERSION == 2.12* ]]; then
    echo "Coverage"
    time sbt $SBT_ARGS coverageReport coverageAggregate
    pip install --user codecov && codecov
fi
show_mem

sleep 10
