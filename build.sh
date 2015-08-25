#!/bin/bash
set -ev
sbt clean coverage test coverageReport coverageAggregate codacyCoverage
