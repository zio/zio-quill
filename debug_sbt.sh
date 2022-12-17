#!/bin/bash

JAVA_OPTS='-Xmx8G -DdebugMacro=true -DexcludeTests=false -Dquill.macro.log.pretty=true -Dquill.macro.log=true -Dquill.trace.enabled=true -Dquill.trace.color=true -Dquill.trace.opinion=false -Dquill.trace.ast.simple=false -Dquill.trace.types='  sbt -jvm-debug 5005
