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

CREATE TABLE TimeEntity(
    sqlDate        DATE,          -- java.sql.Date
    sqlTime        TIME,          -- java.sql.Time
    sqlTimestamp   DATETIME,     -- java.sql.Timestamp
    timeLocalDate      DATE,      -- java.time.LocalDate
    timeLocalTime      TIME,      -- java.time.LocalTime
    timeLocalDateTime  DATETIME, -- java.time.LocalDateTime
    -- DATETIMEOFFSET is SQL Server's equvalent of Postgres TIMESTAMP WITH TIME ZONE
    timeZonedDateTime  DATETIMEOFFSET, -- java.time.ZonedDateTime
    timeInstant        DATETIMEOFFSET, -- java.time.Instant
    -- There is no such thing as a Time+Timezone column in SQL Server
    timeOffsetTime     DATETIMEOFFSET,      -- java.time.OffsetTime
    timeOffsetDateTime DATETIMEOFFSET  -- java.time.OffsetDateTime
);

CREATE TABLE EncodingTestEntity(
    v1 VARCHAR(255),
    v2 DECIMAL(5,2),
    v3 BIT,
    v4 SMALLINT,
    v5 SMALLINT,
    v6 INTEGER,
    v7 BIGINT,
    v8 FLOAT,
    v9 DOUBLE PRECISION,
    v10 VARBINARY(MAX),
    v11 DATETIME,
    v12 VARCHAR(255),
    v13 DATE,
    v14 VARCHAR(255),
    o1 VARCHAR(255),
    o2 DECIMAL(5,2),
    o3 BIT,
    o4 SMALLINT,
    o5 SMALLINT,
    o6 INTEGER,
    o7 BIGINT,
    o8 FLOAT,
    o9 DOUBLE PRECISION,
    o10 VARBINARY(MAX),
    o11 DATETIME,
    o12 VARCHAR(255),
    o13 DATE,
    o14 VARCHAR(255),
    o15 VARCHAR(255)
);

CREATE TABLE TestEntity(
    s VARCHAR(255),
    i INTEGER,
    l BIGINT,
    o INTEGER,
    b BIT
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
    i BIGINT IDENTITY(1,1) PRIMARY KEY,
);

CREATE TABLE Product(
    id INTEGER IDENTITY(1,1) PRIMARY KEY,
    description VARCHAR(255),
    sku BIGINT
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
