#!/bin/bash
chown root ~/.ssh/config
chmod 644 ~/.ssh/config

if [[ $TRAVIS_PULL_REQUEST == "false" && $TRAVIS_BRANCH == "master" ]]
then
	openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/secring.gpg.enc -out local.secring.gpg -d
	openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/pubring.gpg.enc -out local.pubring.gpg -d
	openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/credentials.sbt.enc -out local.credentials.sbt -d
	openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/deploy_key.pem.enc -out local.deploy_key.pem -d

	if [[ $(cat version.sbt) != *"SNAPSHOT"* ]]
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
	else
		sbt clean coverage test tut coverageAggregate release-vcs-checks && sbt coverageOff publish
	fi
else
	sbt clean coverage test tut coverageAggregate release-vcs-checks
fi