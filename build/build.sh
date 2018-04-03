#!/usr/bin/env bash

set -e # Any subsequent(*) commands which fail will cause the shell script to exit immediately
chown root ~/.ssh/config
chmod 644 ~/.ssh/config

SBT_CMD="sbt -DscalaVersion=$SCALA_VERSION ++$SCALA_VERSION clean"

if [[ $SCALA_VERSION == 2.11* ]]
then
    SBT_CMD+=" coverage test tut coverageReport coverageAggregate checkUnformattedFiles"
elif [[ $SCALA_VERSION == 2.12* ]]
then
    SBT_CMD+=" test"
else
    exit 1
fi

if [[ $TRAVIS_PULL_REQUEST == "false" ]]
then
    SBT_CMD+=" coverageOff publish"
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
        $SBT_CMD
    else
        echo "version in ThisBuild := \"$TRAVIS_BRANCH-SNAPSHOT\"" > version.sbt
        $SBT_CMD
    fi
else
    $SBT_CMD
fi