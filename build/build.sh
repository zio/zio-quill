#!/usr/bin/env bash

set -e # Any subsequent(*) commands which fail will cause the shell script to exit immediately
# chown root ~/.ssh/config
# chmod 644 ~/.ssh/config

SCALA_VERSION=$1

sbt_cmd() {
    SBT="sbt -DscalaVersion=$SCALA_VERSION ++$SCALA_VERSION"
    echo $SBT "$@"
    time $SBT "$@"
}

build() {
    sbt_cmd coverage test:compile checkUnformattedFiles tut

    PROJECTS=$(sbt_cmd projects | grep -Po '(quill-[a-zA-Z-]*)')
    echo "Found projects:"
    echo $PROJECTS

    for PROJECT in $PROJECTS
    do
        if [[ $SCALA_VERSION == 2.12* && $PROJECT == "quill-spark" ]]
        then
            echo "Ignoring quill-spark for Scala 2.12"
        else
            retry sbt_cmd coverage $PROJECT/test
        fi
    done
    sbt_cmd coverageReport coverageAggregate
}

retry() {
  local result=0
  local count=1
  while [ $count -le 3 ]; do
    [ $result -ne 0 ] && {
      echo -e "\n${ANSI_RED}The command \"$@\" failed. Retrying, $count of 3.${ANSI_RESET}\n" >&2
    }
    # ! { } ignores set -e, see https://stackoverflow.com/a/4073372
    ! { "$@"; result=$?; }
    [ $result -eq 0 ] && break
    count=$(($count + 1))
    sleep 1
  done

  [ $count -gt 3 ] && {
    echo -e "\n${ANSI_RED}The command \"$@\" failed 3 times.${ANSI_RESET}\n" >&2
  }

  return $result
}

if [[ $TRAVIS_PULL_REQUEST == "false" ]]
then
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/secring.gpg.enc -out local.secring.gpg -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/pubring.gpg.enc -out local.pubring.gpg -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/credentials.sbt.enc -out local.credentials.sbt -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/deploy_key.pem.enc -out local.deploy_key.pem -d

    if [[ $TRAVIS_BRANCH == "master" && $(cat version.sbt) != *"SNAPSHOT"* ]]
    then
        echo Release is scheduled to next jobs
        exit 0
    elif [[ $TRAVIS_BRANCH == "master" ]]
    then
        build
        sbt_cmd publish
    else
        build
        echo "version in ThisBuild := \"$TRAVIS_BRANCH-SNAPSHOT\"" > version.sbt
        sbt_cmd publish
    fi
else
    build
fi