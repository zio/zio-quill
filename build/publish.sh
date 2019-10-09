#!/usr/bin/env bash
set -e # Any subsequent(*) commands which fail will cause the shell script to exit immediately

SBT_CMD="sbt ++2.11.12 -Dquill.macro.log=false -Dquill.scala.version=2.11.12"

echo $SBT_CMD
if [[ $TRAVIS_PULL_REQUEST == "false" ]]
then
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/secring.gpg.enc -out local.secring.gpg -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/pubring.gpg.enc -out local.pubring.gpg -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/credentials.sbt.enc -out local.credentials.sbt.temp -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/deploy_key.pem.enc -out local.deploy_key.pem -d

    # Temp hack to get around SbtPgp being moved. This file needs to be updated with correct import.
    # See https://github.com/sbt/sbt-pgp/pull/162 for more details
    cat local.credentials.sbt.temp | sed 's/import com.typesafe.sbt.SbtPgp/import com.jsuereth.sbtpgp.SbtPgp/' > local.credentials.sbt

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

        # Only Publish Latest from Master Branch otherwise could move 'forward' to wrong version
        git fetch --unshallow
        git checkout master || git checkout -b master
        git reset --hard origin/master
        git push --delete origin website

        # Publish Everything
        $SBT_CMD -Dmodules=none 'release with-defaults default-tag-exists-answer o'
    else
        echo "No-Op Release-Publish for Snapshot Versions"
    fi
fi
