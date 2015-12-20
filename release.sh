#!/bin/bash
if [[ $(git tag -l --contains HEAD) =~ 'release' ]]
then
	eval "$(ssh-agent -s)"
	chmod 600 deploy_key.pem
	ssh-add deploy_key.pem
	sbt release with-defaults
fi
