#!/bin/bash

set -e

source /root/.bashrc

echo "Setting up Oracle"

until source /root/.bashrc; sqlplus quill_test/QuillRocks! < /oracle_setup/external_match_script.sql; do
  echo "Trying Again"
  sleep 5;
done

echo "Oracle Setup Complete"