CREATE TABLE Person(
    name VARCHAR(255),
    age int
);

CREATE TABLE Couple(
    her VARCHAR(255),
    him VARCHAR(255)
);

CREATE TABLE Department(
    dpt VARCHAR(255)
);

CREATE TABLE Employee(
    emp VARCHAR(255),
    dpt VARCHAR(255),
    salary int
);

CREATE TABLE Task(
    emp VARCHAR(255),
    tsk VARCHAR(255)
);

CREATE TABLE EncodingTestEntity(
    v1 VARCHAR(255),
    v2 DECIMAL(5,2),
    v3 BOOLEAN,
    v4 SMALLINT,
    v5 SMALLINT,
    v6 INTEGER,
    v7 BIGINT,
    v8 FLOAT,
    v9 DOUBLE PRECISION,
    v10 BYTEA,
    v11 TIMESTAMP,
    v12 VARCHAR(255),
    v13 DATE,
    v14 UUID,
    o1 VARCHAR(255),
    o2 DECIMAL(5,2),
    o3 BOOLEAN,
    o4 SMALLINT,
    o5 SMALLINT,
    o6 INTEGER,
    o7 BIGINT,
    o8 FLOAT,
    o9 DOUBLE PRECISION,
    o10 BYTEA,
    o11 TIMESTAMP,
    o12 VARCHAR(255),
    o13 DATE,
    o14 UUID
);

CREATE TABLE EncodingUUIDTestEntity(
    v1 UUID
);

CREATE TABLE TestEntity(
    s VARCHAR(255),
    i INTEGER,
    l BIGINT,
    o INTEGER
);

CREATE TABLE TestEntity2(
    s VARCHAR(255),
    i INTEGER,
    l BIGINT
);

CREATE TABLE TestEntity3(
    s VARCHAR(255),
    i INTEGER,
    l BIGINT
);

CREATE TABLE Product(
    description VARCHAR(255),
    id SERIAL PRIMARY KEY,
    sku BIGINT
);

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE Barcode(
    uuid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    description TEXT
);

CREATE TABLE DateEncodingTestEntity (
    v1 DATE,
    v2 TIMESTAMP
);

CREATE TABLE ArraysTestEntity (
    texts TEXT[],
    decimals DECIMAL(5,2)[],
    bools BOOLEAN[],
    bytes SMALLINT[],
    shorts SMALLINT[],
    ints INTEGER[],
    longs BIGINT[],
    floats FLOAT[],
    doubles DOUBLE PRECISION[],
    timestamps TIMESTAMP[],
    dates DATE[],
    uuids UUID[]
);