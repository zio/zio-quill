#!/bin/bash
if [[ $TRAVIS_PULL_REQUEST == "false" && $TRAVIS_BRANCH == "master" ]]
then
	openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in secring.gpg.enc -out local.secring.gpg -d
	openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in pubring.gpg.enc -out local.pubring.gpg -d
	openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in credentials.sbt.enc -out local.credentials.sbt -d
	openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in deploy_key.pem.enc -out local.deploy_key.pem -d

	if [[ $(git tag -l --contains HEAD) =~ 'release' ]]
	then
		chmod 600 local.deploy_key.pem
		docker-compose run sbt bash -c 'eval "$(ssh-agent -s)"'
		docker-compose run openssh ssh-add local.deploy_key.pem
		docker-compose run git git config --global user.name "Quill CI"
		docker-compose run git git config --global user.email "quillci@getquill.io"
		docker-compose run git git remote set-url origin git@github.com:getquill/quill.git
		docker-compose run git git fetch --unshallow
		docker-compose run git git checkout master || git checkout -b master
		docker-compose run git git reset --hard origin/master
		docker-compose run sbt sbt tut 'release with-defaults'
		docker-compose run git git push --delete origin release
	else
		docker-compose run sbt sbt clean coverage test tut coverageAggregate && docker-compose run sbt sbt coverageOff publish
	fi
else
	docker-compose run sbt sbt clean coverage test tut coverageAggregate
fi
