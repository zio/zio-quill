#!/bin/bash
if [[ $TRAVIS_TAG =~ 'release' ]]
then
	eval "$(ssh-agent -s)"
	chmod 600 local.deploy_key.pem
	echo $ENCRYPTION_PASSWORD | ssh-add local.deploy_key.pem
	git config --global user.name "Quill CI"
	git config --global user.email "quillci@getquill.io"
	git remote add origin git@github.com:getquill/quill.git 
	sbt release with-defaults
	git push --delete origin release
fi
