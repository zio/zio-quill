#!/bin/bash

set -e

source /home/oracle/.bashrc
PATH=$ORACLE_HOME/bin:$PATH

echo "Setting up Oracle"

until source /home/oracle/.bashrc; sqlplus quill_test/QuillRocks! < /oracle_setup/external_match_script.sql; do
  echo "Trying Again"
  sleep 5;
done

echo "Oracle Setup Complete"