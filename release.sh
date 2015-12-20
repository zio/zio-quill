#!/bin/bash
if [[ $TRAVIS_TAG =~ 'release' ]]
then
	eval "$(ssh-agent -s)"
	chmod 600 local.deploy_key.pem
	ssh-add local.deploy_key.pem
	git remote add origin git@github.com:getquill/quill.git 
	sbt release with-defaults
	git push --delete origin release
fi
