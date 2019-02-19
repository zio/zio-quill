#!/bin/bash

echo "Running Global Oracle Init"

# Start the oracle database
nohup /entrypoint.sh &

# Save the pid which we need to wait for (otherwise container will exit)
pid=$!

echo "Waiting for Oracle Setup to Complete"

until source /root/.bashrc; sqlplus system/oracle@//localhost:1521/xe < /oracle_setup/external_match_script.sql | grep "match_this_test_to_pass"; do
  echo "Trying Again"
  sleep 5;
done

source /root/.bashrc

echo "Setting Up Test Database"
sqlplus system/oracle@//localhost:1521/xe < /oracle_setup/create_quill_test.sql

echo "Setting Up Test Database Schema"
sqlplus quill_test/QuillRocks! < /quill_setup/oracle-schema.sql

echo "Oracle Setup Complete"

# Wait until oracle DB externally closed
wait $pid
