--
-- The sample data used in the world database is Copyright Statistics
-- Finland, https://www.stat.fi/worldinfigures.
--

CREATE TABLE IF NOT EXISTS city (
    id integer NOT NULL,
    name varchar NOT NULL,
    countrycode character(3) NOT NULL,
    district varchar NOT NULL,
    population integer NOT NULL
);

CREATE TABLE IF NOT EXISTS country (
    code character(3) NOT NULL,
    name varchar NOT NULL,
    continent varchar NOT NULL,
    region varchar NOT NULL,
    surfacearea real NOT NULL,
    indepyear smallint,
    population integer NOT NULL,
    lifeexpectancy real,
    gnp numeric(10,2),
    gnpold numeric(10,2),
    localname varchar NOT NULL,
    governmentform varchar NOT NULL,
    headofstate varchar,
    capital integer,
    code2 character(2) NOT NULL --,
    -- TODO: we can do this with CREATE DOMAIN
    -- CONSTRAINT country_continent_check CHECK ((((((((continent = 'Asia'::text) OR (continent = 'Europe'::text)) OR (continent = 'North America'::text)) OR (continent = 'Africa'::text)) OR (continent = 'Oceania'::text)) OR (continent = 'Antarctica'::text)) OR (continent = 'South America'::text)));
);

CREATE TABLE IF NOT EXISTS countrylanguage (
    countrycode character(3) NOT NULL,
    language varchar NOT NULL,
    isofficial boolean NOT NULL,
    percentage real NOT NULL
);

-- COPY city (id, name, countrycode, district, population) FROM stdin;
INSERT INTO city VALUES (206, 'São Paulo', 'BRA', 'São Paulo', 9968485);
INSERT INTO city VALUES (207, 'Rio de Janeiro', 'BRA', 'Rio de Janeiro', 5598953);
INSERT INTO city VALUES (208, 'Salvador', 'BRA', 'Bahia', 2302832);
INSERT INTO city VALUES (456, 'London', 'GBR', 'England', 7285000);
INSERT INTO city VALUES (457, 'Birmingham', 'GBR', 'England', 1013000);
INSERT INTO city VALUES (458, 'Glasgow', 'GBR', 'Scotland', 619680);
INSERT INTO city VALUES (3793, 'New York', 'USA', 'New York', 8008278);
INSERT INTO city VALUES (3794, 'Los Angeles', 'USA', 'California', 3694820);
INSERT INTO city VALUES (3795, 'Chicago', 'USA', 'Illinois', 2896016);
INSERT INTO city VALUES (3796, 'Houston', 'USA', 'Texas', 1953631);
INSERT INTO city VALUES (3797, 'Philadelphia', 'USA', 'Pennsylvania', 1517550);
INSERT INTO city VALUES (3798, 'Phoenix', 'USA', 'Arizona', 1321045);
INSERT INTO city VALUES (1450, 'Jerusalem', 'ISR', 'Jerusalem', 633700);
INSERT INTO city VALUES (1451, 'Tel Aviv-Jaffa', 'ISR', 'Tel Aviv', 348100);
INSERT INTO city VALUES (1454, 'Beerseba', 'ISR', 'Ha Darom', 163700);


--
-- Data for Name: country; Type: TABLE DATA; Schema: public; Owner: chriskl
--
INSERT INTO country VALUES ('GBR', 'United Kingdom', 'Europe', 'British Islands', 242900, null, 59623400, 77.699997, 1378330.00, 1296830.00, 'United Kingdom', 'Constitutional Monarchy', 'Elisabeth II', 456, 'GB');
INSERT INTO country VALUES ('USA', 'United States', 'North America', 'North America', 9363520, 1776, 278357000, 77.099998, 8510700.00, 8110900.00, 'United States', 'Federal Republic', 'George W. Bush', 3813, 'US');
INSERT INTO country VALUES ('ISR', 'Israel', 'Asia', 'Middle East', 21056, 1948, 6217000, 78.599998, 97477.00, 98577.00, 'Yisrael/Israil', 'Republic', 'Moshe Katzav', 1450, 'IL');
INSERT INTO country VALUES ('BRA', 'Brazil', 'South America', 'South America', 8547403, 1822, 170115000, 62.900002, 776739.00, 804108.00, 'Brasil', 'Federal Republic', 'Fernando Henrique Cardoso', 211, 'BR');
--
-- Data for Name: countrylanguage; Type: TABLE DATA; Schema: public; Owner: chriskl
--
INSERT INTO countrylanguage VALUES ('GBR', 'English', true, 97.300003);
INSERT INTO countrylanguage VALUES ('ISR', 'Hebrew', true, 63.099998);
INSERT INTO countrylanguage VALUES ('USA', 'English', true, 86.199997);
INSERT INTO countrylanguage VALUES ('BRA', 'Portuguese', true, 97.5);

ALTER TABLE city
    ADD CONSTRAINT city_pkey PRIMARY KEY (id);

ALTER TABLE country
    ADD CONSTRAINT country_pkey PRIMARY KEY (code);

ALTER TABLE countrylanguage
    ADD CONSTRAINT countrylanguage_pkey PRIMARY KEY (countrycode, language);

ALTER TABLE country
    ADD CONSTRAINT country_capital_fkey FOREIGN KEY (capital) REFERENCES city(id);

ALTER TABLE countrylanguage
    ADD CONSTRAINT countrylanguage_countrycode_fkey FOREIGN KEY (countrycode) REFERENCES country(code);
