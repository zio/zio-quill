#!/usr/bin/env bash

set -euo pipefail

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo apt-key fingerprint 0EBFCD88
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get -y install "docker-ce=${DOCKER_VERSION}~ce-0~ubuntu-$(lsb_release -cs)"
