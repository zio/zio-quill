#!/usr/bin/env bash

set -e # Any subsequent(*) commands which fail will cause the shell script to exit immediately
chown root ~/.ssh/config
chmod 644 ~/.ssh/config

SBT_CMD="sbt clean"

SBT_CMD_2_11_PREFIX="++2.11.11"
SBT_CMD_2_12_PREFIX="++2.12.2"

SBT_CMD_2_11=" $SBT_CMD_2_11_PREFIX coverage test tut coverageReport coverageAggregate checkUnformattedFiles"
SBT_CMD_2_12=" $SBT_CMD_2_12_PREFIX test"
SBT_PUBLISH=" coverageOff publish"

if [[ $SCALA_VERSION == "2.11" ]]
then
    SBT_CMD+=$SBT_CMD_2_11
elif [[ $SCALA_VERSION == "2.12" ]]
then
    SBT_CMD+=$SBT_CMD_2_12
else
    exit 1
fi

if [[ $TRAVIS_PULL_REQUEST == "false" ]]
then
    SBT_CMD+=$SBT_PUBLISH
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/secring.gpg.enc -out local.secring.gpg -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/pubring.gpg.enc -out local.pubring.gpg -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/credentials.sbt.enc -out local.credentials.sbt -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/deploy_key.pem.enc -out local.deploy_key.pem -d

    if [[ $TRAVIS_BRANCH == "master" && $(cat version.sbt) != *"SNAPSHOT"* ]]
    then
        eval "$(ssh-agent -s)"
        chmod 600 local.deploy_key.pem
        ssh-add local.deploy_key.pem
        git config --global user.name "Quill CI"
        git config --global user.email "quillci@getquill.io"
        git remote set-url origin git@github.com:getquill/quill.git
        git fetch --unshallow
        git checkout master || git checkout -b master
        git reset --hard origin/master

        if [[ $SCALA_VERSION == "2.11" ]]
        then
            git push --delete origin website || true
            sbt $SBT_CMD_2_11_PREFIX tut 'release with-defaults'
        elif [[ $SCALA_VERSION == "2.12" ]]
        then
            sbt $SBT_CMD_2_12_PREFIX tut 'release with-defaults'
        fi
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