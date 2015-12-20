#!/bin/bash
if [[ $(git tag -l --contains HEAD) =~ 'release' ]]
then
	sbt release with-defaults
fi
