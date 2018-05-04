#!/usr/bin/env bash

set -e

BASE=$(basename $PWD)
cd ..
echo "Extracting..."
tar --posix --atime-preserve -zxf /compiled/codebase_2_1*.tar.gz
cd $BASE

SBT_CMD="sbt -DscalaVersion=$SCALA_VERSION ++$SCALA_VERSION quill-coreJVM/test"

if [[ $SCALA_VERSION == $S_2_11 ]]
then
    SBT_CMD+=" "
elif [[ $SCALA_VERSION == $S_2_12 ]]
then
    echo ""
else
    echo unknown scala version $SCALA_VERSION
    exit 1
fi

echo "Compile / skip in ThisBuild := true" >> build.sbt
echo $SBT_CMD
$SBT_CMD

