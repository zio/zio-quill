#!/usr/bin/env bash

if [[ $TRAVIS_PULL_REQUEST == "false" && $TRAVIS_BRANCH == "master" ]]
then
    docker login -u $DOCKER_USER -p $DOCKER_PASSWORD
    docker-compose push
fi
