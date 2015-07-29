#!/bin/bash
set -ev
sbt clean coverage test
sbt coverageReport
sbt coverageAggregate
sbt codacyCoverage
