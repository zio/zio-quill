#!/bin/bash
if [[ $(git tag -l --contains HEAD) =~ 'release' ]]
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
	git symbolic-ref HEAD
	sbt tut
	sbt 'release with-defaults'
	git push --delete deploy release
else
	sbt clean coverage test tut coverageAggregate
fi
