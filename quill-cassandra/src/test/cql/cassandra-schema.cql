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
    ints LIST<INT>,
    longs LIST<BIGINT>,
    floats LIST<FLOAT>,
    doubles LIST<DOUBLE>
);

CREATE TABLE SetsEntity(
    id INT PRIMARY KEY,
    texts Set<VARCHAR>,
    decimals Set<DECIMAL>,
    bools Set<BOOLEAN>,
    ints Set<INT>,
    longs Set<BIGINT>,
    floats Set<FLOAT>,
    doubles Set<DOUBLE>
);

CREATE TABLE MapsEntity(
    id INT PRIMARY KEY,
    textsDoubles Map<VARCHAR, DOUBLE>,
    intsBools Map<INT, BOOLEAN>,
    longsFloats Map<BIGINT, FLOAT>
);