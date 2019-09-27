CREATE TABLE Person(
    name VARCHAR(255),
    age int(3)
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
    salary int(20)
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
    v9 DOUBLE,
    v10 VARBINARY(255),
    v11 DATETIME,
    v12 VARCHAR(255),
    v13 DATE,
    v14 VARCHAR(255),
    o1 VARCHAR(255),
    o2 DECIMAL(5,2),
    o3 BOOLEAN,
    o4 SMALLINT,
    o5 SMALLINT,
    o6 INTEGER,
    o7 BIGINT,
    o8 FLOAT,
    o9 DOUBLE,
    o10 VARBINARY(255),
    o11 DATETIME,
    o12 VARCHAR(255),
    o13 DATE,
    o14 VARCHAR(255),
    o15 VARCHAR(255)
);

Create TABLE DateEncodingTestEntity(
    v1 date,
    v2 datetime(6),
    v3 timestamp(6)
);

Create TABLE LocalDateTimeEncodingTestEntity(
    v1 datetime,
    v2 timestamp
);

Create TABLE BooleanEncodingTestEntity(
    v1 BOOLEAN,
    v2 BIT(1),
    v3 TINYINT,
    v4 SMALLINT,
    v5 MEDIUMINT,
    v6 INT,
    v7 BIGINT
);

CREATE TABLE TestEntity(
    s VARCHAR(255),
    i INTEGER primary key,
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

CREATE TABLE TestEntity4(
    i BIGINT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (i)
);

CREATE TABLE Product(
    description VARCHAR(255),
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE Contact(
    firstName VARCHAR(255),
    lastName VARCHAR(255),
    age int,
    addressFk int,
    extraInfo VARCHAR(255)
);

CREATE TABLE Address(
    id int,
    street VARCHAR(255),
    zip int,
    otherExtraInfo VARCHAR(255)
);
