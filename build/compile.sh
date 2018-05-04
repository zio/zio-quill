#!/usr/bin/env bash

set -e

SBT_CMD="sbt -DscalaVersion=$SCALA_VERSION ++$SCALA_VERSION clean"
ARCHIVE_PATH="$HOME/compiled/codebase"

if [[ $SCALA_VERSION == $S_2_11 ]]
then
    SBT_CMD+=" coverage quill-coreJVM/test:compile"
    ARCHIVE_PATH+="_2_11.tar.gz"
elif [[ $SCALA_VERSION == $S_2_12 ]]
then
    SBT_CMD+=" quill-coreJVM/test:compile"
    ARCHIVE_PATH+="_2_12.tar.gz"
else
    echo unknown scala version $SCALA_VERSION
    exit 1
fi
echo Working dir is $PWD

$SBT_CMD

BASE=$(basename $PWD)
cd ..
echo "Achieving..."
tar --posix --atime-preserve -zpcf $ARCHIVE_PATH $BASE
cd $BASE