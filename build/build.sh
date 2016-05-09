#!/bin/bash
set -e # Any subsequent(*) commands which fail will cause the shell script to exit immediately
chown root ~/.ssh/config
chmod 644 ~/.ssh/config

if [[ $TRAVIS_PULL_REQUEST == "false" ]]
then
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
		sbt tut 'release with-defaults'
		git push --delete origin release
	elif [[ $TRAVIS_BRANCH == "master" ]]
	then
		sbt clean coverage test tut coverageAggregate release-vcs-checks
		sbt coverageOff publish
	else
		sbt clean coverage test tut coverageAggregate release-vcs-checks
		echo "version in ThisBuild := \"$TRAVIS_BRANCH-SNAPSHOT\"" > version.sbt
		sbt coverageOff publish
	fi
else
	sbt clean coverage test tut coverageAggregate release-vcs-checks
fi