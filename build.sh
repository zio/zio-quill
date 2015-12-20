#!/bin/bash
if [[ $TRAVIS_TAG -eq 'release' ]]
then
	./add_ssh_key
	git config --global user.name "Quill CI"
	git config --global user.email "quillci@getquill.io"
	git remote add origin git@github.com:getquill/quill.git 
	sbt clean release with-defaults
	git push --delete origin release
else
	sbt clean coverage test tut coverageAggregate
fi
