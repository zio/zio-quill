CREATE TABLE IF NOT EXISTS Person(
    name VARCHAR(255),
    age int
);

CREATE TABLE IF NOT EXISTS Couple(
    her VARCHAR(255),
    him VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS Department(
    dpt VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS Employee(
    emp VARCHAR(255),
    dpt VARCHAR(255),
    salary int
);

CREATE TABLE IF NOT EXISTS Task(
    emp VARCHAR(255),
    tsk VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS TimeEntity(
    sqlDate        DATE,                     -- java.sql.Date
    sqlTime        TIME,                     -- java.sql.Time
    sqlTimestamp   TIMESTAMP,                -- java.sql.Timestamp
    timeLocalDate      DATE,                     -- java.time.LocalDate
    timeLocalTime      TIME,                     -- java.time.LocalTime
    timeLocalDateTime  TIMESTAMP,                -- java.time.LocalDateTime
    timeZonedDateTime  TIMESTAMP WITH TIME ZONE, -- java.time.ZonedDateTime
    timeInstant        TIMESTAMP WITH TIME ZONE, -- java.time.Instant
    -- Like Postgres, H2 actually has a notion of a Time+Timezone type unlike most DBs
    timeOffsetTime     TIME WITH TIME ZONE,      -- java.time.OffsetTime
    timeOffsetDateTime TIMESTAMP WITH TIME ZONE  -- java.time.OffsetDateTime
);

CREATE TABLE IF NOT EXISTS EncodingTestEntity(
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
    o14 UUID,
    o15 VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS TestEntity(
    s VARCHAR(255),
    i INTEGER,
    l BIGINT,
    o INTEGER,
    b BOOLEAN
);

CREATE TABLE IF NOT EXISTS TestEntity2(
    s VARCHAR(255),
    i INTEGER,
    l BIGINT
);

CREATE TABLE IF NOT EXISTS TestEntity3(
    s VARCHAR(255),
    i INTEGER,
    l BIGINT
);

CREATE TABLE IF NOT EXISTS TestEntity4(
    i identity
);

CREATE TABLE IF NOT EXISTS Product(
    description VARCHAR(255),
    id identity,
    sku BIGINT
);

CREATE TABLE IF NOT EXISTS ArraysTestEntity (
    texts VARCHAR(255) ARRAY,
    decimals   DECIMAL(5,2) ARRAY,
    bools      BOOLEAN ARRAY,
    bytes      TINYINT ARRAY,
    shorts     SMALLINT ARRAY,
    ints       INT ARRAY,
    longs      BIGINT ARRAY,
    floats     FLOAT ARRAY,
    doubles    DOUBLE PRECISION ARRAY,
    timestamps TIMESTAMP ARRAY,
    dates      DATE ARRAY,
    uuids      UUID ARRAY
);

CREATE TABLE IF NOT EXISTS Contact(
    firstName VARCHAR(255),
    lastName VARCHAR(255),
    age int,
    addressFk int,
    extraInfo VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS Address(
    id int,
    street VARCHAR(255),
    zip int,
    otherExtraInfo VARCHAR(255)
);
