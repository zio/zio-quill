---
id: Code_Generation
title: Code Generation
---


Quill now has a highly customizable code generator. Currently, it only supports JDBC but it will soon
be extended to other contexts. With a minimal amount of configuration, the code generator takes schemas like this:

```sql
-- Using schema 'public'

create table public.Person (
  id int primary key auto_increment,
  first_name varchar(255),
  last_name varchar(255),
  age int not null
);

create table public.Address (
  person_fk int not null,
  street varchar(255),
  zip int
);
```

Producing objects like this:

```scala
// src/main/scala/com/my/project/public/Person.scala
package com.my.project.public
  
case class Person(id: Int, firstName: Option[String], lastName: Option[String], age: Int)
```

```scala
// src/main/scala/com/my/project/public/Address.scala
package com.my.project.public
  
case class Address(personFk: Int, street: Option[String], zip: Option[Int])
```

Have a look at the [CODEGEN.md](https://github.com/getquill/quill/blob/master/CODEGEN.md) manual page for more details.

#### sbt dependencies

```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-codegen-jdbc" % "3.5.1-SNAPSHOT"
)
```