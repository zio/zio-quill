-- init.sql
STARTUP;

CREATE USER secretsysuser
    IDENTIFIED BY secretpassword
    DEFAULT TABLESPACE SYSTEM
    TEMPORARY TABLESPACE SYSTEM
    PROFILE DEFAULT
    ACCOUNT UNLOCK;

-- 1 Role for secretsysuser 
GRANT DBA TO secretsysuser;
ALTER USER secretsysuser DEFAULT ROLE ALL;

-- 2 System Privileges for secretsysuser 
GRANT CREATE SESSION TO secretsysuser;
GRANT UNLIMITED TABLESPACE TO secretsysuser;

-- Other test users
CREATE USER quill_test IDENTIFIED BY "QuillRocks!" QUOTA 50M ON system;
GRANT DBA TO quill_test;

CREATE USER codegen_test IDENTIFIED BY "QuillRocks!" QUOTA 50M ON system;
GRANT DBA TO codegen_test;

CREATE USER alpha IDENTIFIED BY "QuillRocks!" QUOTA 50M ON system;
GRANT DBA TO alpha;

CREATE USER bravo IDENTIFIED BY "QuillRocks!" QUOTA 50M ON system;
GRANT DBA TO bravo;

BEGIN
    dbms_service.create_service('XE','XE');
dbms_service.start_service('XE');
END;
/

CREATE OR REPLACE TRIGGER SYS.ROLE_TRIGGER AFTER STARTUP ON DATABASE
BEGIN
    dbms_service.start_service('XE');
END;
/

EXIT;
