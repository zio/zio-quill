#!/usr/bin/env bash
set -e # Any subsequent(*) commands which fail will cause the shell script to exit immediately

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
        git push --delete origin website
    fi
fi
