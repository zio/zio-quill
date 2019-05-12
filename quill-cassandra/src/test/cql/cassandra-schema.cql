CREATE KEYSPACE IF NOT EXISTS quill_test
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

use quill_test;

CREATE TABLE Person(
    id INT PRIMARY KEY,
    name VARCHAR,
    age INT
);

CREATE TABLE Couple(
    id INT PRIMARY KEY,
    her VARCHAR,
    him VARCHAR
);

CREATE TABLE Department(
    id INT PRIMARY KEY,
    dpt VARCHAR
);

CREATE TABLE Employee(
    id INT PRIMARY KEY,
    emp VARCHAR,
    dpt VARCHAR,
    salary INT
);

CREATE TABLE Task(
    id INT PRIMARY KEY,
    emp VARCHAR,
    tsk VARCHAR
);

CREATE TABLE DecodeNullTestEntity(
    id INT PRIMARY KEY,
    value INT
);

CREATE TABLE EncodingTestEntity(
    id INT,
    v1 VARCHAR,
    v2 DECIMAL,
    v3 BOOLEAN,
    v4 INT,
    v5 BIGINT,
    v6 FLOAT,
    v7 DOUBLE,
    v8 BLOB,
    v9 DATE,
    v10 UUID,
    v11 TIMESTAMP,
    v12 TINYINT,
    v13 SMALLINT,
    o1 VARCHAR,
    o2 DECIMAL,
    o3 BOOLEAN,
    o4 INT,
    o5 BIGINT,
    o6 FLOAT,
    o7 DOUBLE,
    o8 BLOB,
    o9 TIMESTAMP,
    o10 DATE,
    PRIMARY KEY(id, v1)
);

CREATE TABLE OrderTestEntity(
    id INT,
    i Int,
    PRIMARY KEY(id, i)
) WITH CLUSTERING ORDER BY (i ASC);

CREATE TABLE TestEntity(
    id INT PRIMARY KEY,
    s VARCHAR,
    i INT,
    l BIGINT,
    o INT
);

CREATE TABLE TestEntity2(
    id INT PRIMARY KEY,
    s VARCHAR,
    i INT,
    l BIGINT
);

CREATE TABLE TestEntity3(
    id INT PRIMARY KEY,
    s VARCHAR,
    i INT,
    l BIGINT
);

CREATE TABLE ListsEntity(
    id INT PRIMARY KEY,
    texts LIST<VARCHAR>,
    decimals LIST<DECIMAL>,
    bools LIST<BOOLEAN>,
    bytes LIST<TINYINT>,
    shorts LIST<SMALLINT>,
    ints LIST<INT>,
    longs LIST<BIGINT>,
    floats LIST<FLOAT>,
    doubles LIST<DOUBLE>,
    blobs LIST<BLOB>,
    dates LIST<DATE>,
    timestamps LIST<TIMESTAMP>,
    uuids LIST<UUID>
);

CREATE TABLE ListFrozen(
    id frozen <LIST<INT>> PRIMARY KEY
);

CREATE TABLE SetsEntity(
    id INT PRIMARY KEY,
    texts SET<VARCHAR>,
    decimals SET<DECIMAL>,
    bools SET<BOOLEAN>,
    ints SET<INT>,
    longs SET<BIGINT>,
    floats SET<FLOAT>,
    doubles SET<DOUBLE>,
    blobs SET<BLOB>,
    dates SET<DATE>,
    timestamps SET<TIMESTAMP>,
    uuids SET<UUID>
);

CREATE TABLE SetFrozen(
    id frozen <SET<INT>> PRIMARY KEY
);

CREATE TABLE MapsEntity(
    id INT PRIMARY KEY,
    textDecimal MAP<VARCHAR, DECIMAL>,
    intDouble MAP<INT, DOUBLE>,
    longFloat MAP<BIGINT, FLOAT>,
    boolDate MAP<BOOLEAN, DATE>,
    uuidTimestamp MAP<UUID, TIMESTAMP>
);

CREATE TABLE MapFrozen(
    id frozen <MAP<INT, BOOLEAN>> PRIMARY KEY
);

CREATE TYPE Name(
    firstName TEXT,
    lastName TEXT
);

CREATE TYPE Personal(
    number INT,
    street TEXT,
    name frozen<Name>,
    optName frozen<Name>,
    list LIST<TEXT>,
    sets SET<INT>,
    map MAP<INT, TEXT>
);

CREATE TABLE WithEverything(
    id INT PRIMARY KEY,
    name Name,
    frozenName frozen <Name>,
    nameList LIST<frozen <Name>>,
    nameSet SET<frozen <Name>>,
    nameMap MAP<INT, frozen <Name>>,
    personal frozen<Personal>
);

CREATE KEYSPACE quill_test_2 WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TYPE quill_test_2.name(
    first_name TEXT,
    last_name TEXT
);

CREATE TABLE quill_test_2.with_udt(
    id INT PRIMARY KEY,
    name frozen <quill_test_2.Name>
);

CREATE TABLE Contact(
    id INT PRIMARY KEY,
    firstName VARCHAR,
    lastName VARCHAR,
    age INT,
    addressFk INT,
    extraInfo VARCHAR
);

CREATE TABLE Address(
    id INT PRIMARY KEY,
    street VARCHAR,
    zip INT,
    otherExtraInfo VARCHAR
);
