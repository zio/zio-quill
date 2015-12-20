#!/bin/bash
if [[ $TRAVIS_TAG =~ 'release' ]]
then
	eval "$(ssh-agent -s)"
	chmod 600 local.deploy_key.pem
	ssh-add local.deploy_key.pem
	git config --global user.name "Quill CI"
	git config --global user.email "quillci@getquill.io"
	git remote set-url origin git@github.com:getquill/quill.git 
	git fetch
	git checkout master
	sbt clean release with-defaults
	git push --delete deploy release
else
	sbt clean coverage test tut coverageAggregate
fi
