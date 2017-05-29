#!/usr/bin/env bash

# Cleanup the cached directories to avoid unnecessary cache updates
sudo find $HOME/.ivy2/cache -name "ivydata-*.properties" -print -delete > /dev/null
sudo find $HOME/.sbt        -name "*.lock"               -print -delete > /dev/null
