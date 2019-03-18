alter session set "_ORACLE_SCRIPT"=true;
CREATE USER quill_test IDENTIFIED BY "QuillRocks!" QUOTA 50M ON system;
GRANT DBA TO quill_test;

CREATE USER codegen_test IDENTIFIED BY "QuillRocks!" QUOTA 50M ON system;
GRANT DBA TO codegen_test;

CREATE USER alpha IDENTIFIED BY "QuillRocks!" QUOTA 50M ON system;
GRANT DBA TO alpha;

CREATE USER bravo IDENTIFIED BY "QuillRocks!" QUOTA 50M ON system;
GRANT DBA TO bravo;
