DROP DATABASE IF EXISTS PEOPLE;
CREATE DATABASE PEOPLE;
USE PEOPLE;

CREATE TABLE Person(
	name varchar(255),
	age int(3)
);

CREATE TABLE Couple(
	her varchar(255),
	him varchar(255)
);

INSERT INTO Person(name, age) VALUES ('Alex', 60);
INSERT INTO Person(name, age) VALUES ('Bert', 55);
INSERT INTO Person(name, age) VALUES ('Cora', 33);
INSERT INTO Person(name, age) VALUES ('Drew', 31);
INSERT INTO Person(name, age) VALUES ('Edna', 21);
INSERT INTO Person(name, age) VALUES ('Fred', 60);

INSERT INTO Couple(her, him) VALUES ('Alex', 'Bert');
INSERT INTO Couple(her, him) VALUES ('Cora', 'Drew');
INSERT INTO Couple(her, him) VALUES ('Edna', 'Fred');