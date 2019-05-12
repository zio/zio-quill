create table Person (
  id int primary key,
  first_name varchar(255),
  last_name varchar(255),
  age int not null
);

create table Address (
  person_fk int not null,
  street varchar(255),
  zip int
);

insert into Person values (1, 'Joe', 'Bloggs', 22);
insert into Person values (2, 'Jack', 'Ripper', 33);
insert into Address values (1, '123 Someplace', 1001);
insert into Address values (1, '678 Blah', 2002);
insert into Address values (2, '111234 Some Other Place', 3333);