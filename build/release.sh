#!/usr/bin/env bash
set -e # Any subsequent(*) commands which fail will cause the shell script to exit immediately

SBT_2_12="sbt ++2.12.6 -Dquill.macro.log=false"
SBT_2_11="sbt ++2.11.12 -Dquill.macro.log=false"

echo $SBT_CMD
if [[ $TRAVIS_PULL_REQUEST == "false" ]]
then
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/secring.gpg.enc -out local.secring.gpg -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/pubring.gpg.enc -out local.pubring.gpg -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/credentials.sbt.enc -out local.credentials.sbt -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/deploy_key.pem.enc -out local.deploy_key.pem -d

    ls -ltr
    sleep 3 # Need to wait until credential files fully written or build fails sometimes

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
        git push --delete origin website || true

        $SBT_2_12 -Dmodules=base -DskipPush=true 'release with-defaults'
        $SBT_2_12 -Dmodules=db -DskipPush=true 'release with-defaults'
        $SBT_2_12 -Dmodules=async -DskipPush=true 'release with-defaults'
        $SBT_2_12 -Dmodules=codegen -DskipPush=true 'release with-defaults'
        echo "Release 2.12 Version:"
        cat version.sbt
        if ! output=$($SBT_2_12 -Dmodules=bigdata 'release with-defaults default-tag-exists-answer o'); then
          echo "Release 2.12 Version After:"
          cat version.sbt
          exit $?
        fi

        $SBT_2_11 -Dmodules=base -DskipPush=true 'release with-defaults'
        $SBT_2_11 -Dmodules=db -DskipPush=true 'release with-defaults'
        $SBT_2_11 -Dmodules=async -DskipPush=true 'release with-defaults'
        $SBT_2_11 -Dmodules=codegen -DskipPush=true 'release with-defaults'
        echo "Release 2.11 Version:"
        cat version.sbt
        if ! output=$($SBT_2_11 -Dmodules=bigdata 'release with-defaults default-tag-exists-answer o'); then
          echo "Release 2.11 Version After:"
          cat version.sbt
          exit $?
        fi

    elif [[ $TRAVIS_BRANCH == "master" ]]
    then
        $SBT_2_12 -Dmodules=base publish
        $SBT_2_12 -Dmodules=db publish
        $SBT_2_12 -Dmodules=async publish
        $SBT_2_12 -Dmodules=codegen publish
        $SBT_2_12 -Dmodules=bigdata publish
        $SBT_2_11 -Dmodules=base publish
        $SBT_2_11 -Dmodules=db publish
        $SBT_2_11 -Dmodules=async publish
        $SBT_2_11 -Dmodules=codegen publish
        $SBT_2_11 -Dmodules=bigdata publish
    else
        echo "version in ThisBuild := \"$TRAVIS_BRANCH-SNAPSHOT\"" > version.sbt
        $SBT_2_12 -Dmodules=base publish
        $SBT_2_12 -Dmodules=db publish
        $SBT_2_12 -Dmodules=async publish
        $SBT_2_12 -Dmodules=codegen publish
        $SBT_2_12 -Dmodules=bigdata publish
        $SBT_2_11 -Dmodules=base publish
        $SBT_2_11 -Dmodules=db publish
        $SBT_2_11 -Dmodules=async publish
        $SBT_2_11 -Dmodules=codegen publish
        $SBT_2_11 -Dmodules=bigdata publish
    fi
fi
