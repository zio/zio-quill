create table Alpha_Person (
  id int primary key,
  first_name varchar(255),
  last_name varchar(255),
  age int not null,
  foo varchar(255),
  num_trinkets int, -- The other one is a bigint so output needs to be a bigint (i.e. Long) but this one is nullable so output must be nullable (i.e. Option)
  trinket_type varchar(255) not null -- Other one is an int but have to make datatype String since this one is a string. Since neither is nullable don't need Option.
);

create table Bravo_Person (
  id int primary key,
  first_name varchar(255),
  bar varchar(255),
  last_name varchar(255),
  age int not null,
  num_trinkets bigint not null,
  trinket_type int not null
);

create table Address (
  person_fk int not null,
  street varchar(255),
  zip int not null
);

insert into Alpha_Person values (1, 'Joe', 'Bloggs', 22, 'blah', 55, 'Wonkles');
insert into Alpha_Person values (2, 'Jack', 'Ripper', 33, 'blah', 66, 'Ginkles');

insert into Bravo_Person values (1, 'George', 'blah', 'Oleaf', 22, 77, 1);
insert into Bravo_Person values (2, 'Greg', 'blah', 'Raynor', 33, 88, 2);

insert into Address values (1, '123 Someplace', 1001);
insert into Address values (1, '678 Blah', 2002);
insert into Address values (2, '111234 Some Other Place', 3333);