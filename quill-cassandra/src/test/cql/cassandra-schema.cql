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

CREATE TABLE EncodingTestEntity(
    id INT PRIMARY KEY,
	v1 VARCHAR,
    v2 DECIMAL,
    v3 BOOLEAN,
    v4 INT,
    v5 BIGINT,
    v6 FLOAT,
    v7 DOUBLE,
    v8 BLOB,
    v9 TIMESTAMP,
    v10 UUID,
    o1 VARCHAR,
    o2 DECIMAL,
    o3 BOOLEAN,
    o4 INT,
    o5 BIGINT,
    o6 FLOAT,
    o7 DOUBLE,
    o8 BLOB,
    o9 TIMESTAMP
);

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