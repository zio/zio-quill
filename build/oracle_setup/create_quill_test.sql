alter session set "_ORACLE_SCRIPT"=true;
CREATE USER quill_test IDENTIFIED BY "QuillRocks!" QUOTA 50M ON system;
GRANT DBA TO quill_test;
