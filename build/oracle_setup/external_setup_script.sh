#!/bin/bash

echo "Running Global Oracle Init"
nohup ${ORACLE_BASE}/scripts/${RUN_FILE} &

# Save the pid which we need to wait for (otherwise container will exit)
pid=$!

PATH=$ORACLE_HOME/bin:$PATH

echo "Waiting for Oracle Setup to Complete"

until source /home/oracle/.bashrc; $ORACLE_BASE/scripts/$CHECK_DB_FILE; do
  echo "Oracle not running yet, Trying Again"
  sleep 5;
done

source /home/oracle/.bashrc

echo "Setting Up Test Database"
sqlplus sys/Oracle18@localhost/XE as sysdba < /oracle_setup/create_quill_test.sql

echo "Setting Up Test Database Schema"
sqlplus quill_test/QuillRocks! < /quill_setup/oracle-schema.sql

echo "Oracle Setup Complete"

# Wait until oracle DB externally closed
wait $pid
