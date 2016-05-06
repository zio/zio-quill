###IMPORTANT: This is the documentation for the latest `SNAPSHOT` version. Please refer to the website at [http://getquill.io](http://getquill.io) for the lastest release's documentation.###

![quill](https://raw.githubusercontent.com/getquill/quill/master/quill.png)
# Quill
Compile-time Language Integrated Query for Scala

[![Build Status](https://travis-ci.org/getquill/quill.svg?branch=master)](https://travis-ci.org/getquill/quill)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/36ab84c7ff43480489df9b7312a4bdc1)](https://www.codacy.com/app/fwbrasil/quill)
[![codecov.io](https://codecov.io/github/getquill/quill/coverage.svg?branch=master)](https://codecov.io/github/getquill/quill?branch=master)
[![Join the chat at https://gitter.im/getquill/quill](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/getquill/quill?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Dependency Status](https://www.versioneye.com/user/projects/56ea4da64e714c0035e76353/badge.svg?style=flat)](https://www.versioneye.com/user/projects/56ea4da64e714c0035e76353)

Quill provides a Quoted Domain Specific Language ([QDSL](http://homepages.inf.ed.ac.uk/slindley/papers/qdsl-draft-february2015.pdf)) to express queries in Scala and execute them in a target language. The library's core is designed to support multiple target languages, currently featuring specializations for Structured Query Language ([SQL](https://en.wikipedia.org/wiki/SQL)) and Cassandra Query Language ([CQL](https://cassandra.apache.org/doc/cql3/CQL.html#selectStmt)).

![example](https://raw.githubusercontent.com/getquill/quill/master/example.gif)

1. **Boilerplate-free mapping**: The database schema is mapped using simple case classes.
2. **Quoted DSL**: Queries are defined inside a `quote` block. Quill parses each quoted block of code (quotation) at compile time and translates them to an internal Abstract Syntax Tree (AST)
3. **Compile-time query generation**: The `db.run` call reads the quotation's AST and translates it to the target language at compile time, emitting the query string as a compilation message. As the query string is known at compile time, the runtime overhead is very low and similar to using the database driver directly.
4. **Compile-time query validation**: If configured, the query is verified against the database at compile time and the compilation fails if it is not valid. The query validation **does not** alter the database state.

# Index #

* [Quotation](#quotation)
* [Mirror sources](#mirror-sources)
* [Compile-time quotations](#compile-time-quotations)
* [Parametrized quotations](#parametrized-quotations)
* [Schema](#schema)
* [Queries](#queries)
* [Query probing](#query-probing)
* [Actions](#actions)
* [Dynamic queries](#dynamic-queries)
* [SQL-specific operations](#sql-specific-operations)
* [Cassandra-specific operations](#cassandra-specific-operations)
* [Extending quill](#extending-quill)
  * [Infix](#infix)
  * [Custom encoding](#custom-encoding)
* [Sql Sources](#sql-sources)
  * [Dialect](#dialect)
  * [Naming strategy](#naming-strategy)
  * [Configuration](#configuration)
* [Cassandra sources](#cassandra-sources)
* [Templates](#templates)
* [Acknowledgments](#acknowledgments)
* [Code of Conduct](#code-of-conduct)
* [License](#license)

# Quotation #

The QDSL allows the user to write plain Scala code, leveraging scala's syntax and type system. Quotations are created using the `quote` method and can contain any excerpt of code that uses supported operations. To create quotations, first import `quote` and some other auxiliary methods:

```scala
import io.getquill._
```

A quotation can be a simple value:

```scala
val pi = quote(3.14159)
```

And be used within another quotation:

```scala
case class Circle(radius: Float)

val areas = quote {
  query[Circle].map(c => pi * c.radius * c.radius)
}
```

Quotations can also contain high-order functions and inline values:

```scala
val area = quote {
  (c: Circle) => {
    val r2 = c.radius * c.radius
    pi * r2
  }
}
```

```scala
val areas = quote {
  query[Circle].map(c => area(c))
}
```

Quotations can contain values defined outside of the quotation:
```scala
val pi = 3.14159
val areas = quote {
  query[Circle].map(c => pi * c.radius * c.radius)
}
```

Quill's normalization engine applies reduction steps before translating the quotation to the target language. The correspondent normalized quotation for both versions of the `areas` query is:

```scala
val areas = quote {
  query[Circle].map(c => 3.14159 * c.radius * c.radius)
}
```

Scala doesn't have support for high-order functions with type parameters. Quill supports anonymous classes with an apply method for this purpose:

```scala
val existsAny = quote {
  new {
    def apply[T](xs: Query[T])(p: T => Boolean) =
    	xs.filter(p(_)).nonEmpty
  }
}

val q = quote {
  query[Circle].filter { c1 => 
    existsAny(query[Circle])(c2 => c2.radius > c1.radius)
  }
}
```

# Mirror sources #

Sources represent the database and provide an execution interface for queries. Quill provides mirror sources for test purposes. Please refer to [sources](#sources) for information on how to create normal sources.

Instead of running the query, mirror sources return a structure with the information that would be used to run the query. There are three mirror source configurations:

- `io.getquill.MirrorSourceConfig`: Mirrors the quotation AST
- `io.getquill.SqlMirrorSourceConfig`: Mirrors the SQL query
- `io.getquill.CassandraMirrorSourceConfig`: Mirrors the CQL query

This documentation uses the SQL mirror in its examples under the `db` name:

```scala
import io.getquill._

lazy val db = source(new SqlMirrorSourceConfig("testSource"))
```

# Compile-time quotations #

Quotations are both compile-time and runtime values. Quill uses a type refinement to store the quotation's AST as an annotation available at compile-time and the `q.ast` method exposes the AST as runtime value.

It is important to avoid giving explicit types to quotations when possible. For instance, this quotation can't be read at compile-time as the type refinement is lost:

```scala
// Avoid type widening (Quoted[Query[Circle]]), or else the quotation will be dynamic.
val q: Quoted[Query[Circle]] = quote {
  query[Circle].filter(c => c.radius > 10)
}

db.run(q) // Dynamic query
```

Quill falls back to runtime normalization and query generation if the quotation's AST can be read at compile-time. Please refer to [dynamic queries](#dynamic-queries) for more information

# Bindings #

Quotations are designed to be self-contained, without references to runtime values outside their scope. There are two mechanisms to explicitly bind runtime values to a quotation execution.

## Lifted values ##

A runtime value can be lifted to a quotation through the method `lift`:

```scala
def biggerThan(i: Float) = quote {
  query[Circle].filter(r => r.radius > lift(i))
}
db.run(biggerThan(10)) // SELECT r.radius FROM Circle r WHERE r.radius > ?
```

## Parametrized quotations ##

A quotation can be defined as a function:

```scala
val biggerThan = quote {
  (i: Int) =>
    query[Circle].filter(r => r.radius > i)
}
```

And a runtime value can be specified when running it:

```scala
db.run(biggerThan)(10) // SELECT r.radius FROM Circle r WHERE r.radius > ?
```

# Schema #

The database schema is represented by case classes. By default, quill uses the class and field names as the database identifiers:

```scala
case class Circle(radius: Float)

val q = quote {
  query[Circle].filter(c => c.radius > 1)
}

db.run(q) // SELECT c.radius FROM Circle c WHERE c.radius > 1
```

Alternatively, the identifiers can be customized:

```scala
val circles = quote {
  query[Circle](_.entity("circle_table").columns(_.radius -> "radius_column"))
}

val q = quote {
  circles.filter(c => c.radius > 1)
}

db.run(q) 
// SELECT c.radius_column FROM circle_table c WHERE c.radius_column > 1
```

If multiple tables require custom identifiers, it is good practice to define a `schema` object with all table queries to be reused across multiple queries:

```scala
case class Circle(radius: Int)
case class Rectangle(length: Int, width: Int)
object schema {
  val circles = quote {
    query[Circle](
        _.entity("circle_table")
        .columns(_.radius -> "radius_column"))
  }
  val rectangles = quote {
    query[Rectangle](
        _.entity("rectangle_table")
        .columns(
          _.length -> "length_column",
          _.width -> "width_column"))
  }
}
```

It is possible to define a column that is a key generated by the database. It will be ignored during insertions and returned as the result.
Note that it accepts only values that can be read as `Long`.
```scala
case class Product(id: Long, description: String, sku: Long)

val q = quote {
  query[Product](_.generated(_.id)).insert
}

db.run(q)
// INSERT INTO Product (description,sku) VALUES (?, ?)
```

# Queries #

The overall abstraction of quill queries is use database tables as if they were in-memory collections. Scala for-comprehensions provide syntatic sugar to deal with this kind of monadic operations:

```scala
case class Person(id: Int, name: String, age: Int)
case class Contact(personId: Int, phone: String)

val q = quote {
  for {
    p <- query[Person] if(p.id == 999)
    c <- query[Contact] if(c.personId == p.id)
  } yield {
    (p.name, c.phone)
  }
}

db.run(q) 
// SELECT p.name, c.phone FROM Person p, Contact c WHERE (p.id = 999) AND (c.personId = p.id)
```

Quill normalizes the quotation and translates the monadic joins to applicative joins, generating a database-friendly query that avoids nested queries.

Any of the following features can be used together with the others and/or within a for-comprehension:

**filter**
```scala
val q = quote {
  query[Person].filter(p => p.age > 18)
}

db.run(q)
// SELECT p.id, p.name, p.age FROM Person p WHERE p.age > 18
```

**map**
```scala
val q = quote {
  query[Person].map(p => p.name)
}

db.run(q)
// SELECT p.name FROM Person p
```

**flatMap**
```scala
val q = quote {
  query[Person].filter(p => p.age > 18).flatMap(p => query[Contact].filter(c => c.personId == p.id))
}

db.run(q)
// SELECT c.personId, c.phone FROM Person p, Contact c WHERE (p.age > 18) AND (c.personId = p.id)
```

**sortBy**
```scala
val q1 = quote {
  query[Person].sortBy(p => p.age)
}

db.run(q1)
// SELECT p.id, p.name, p.age FROM Person p ORDER BY p.age ASC NULLS FIRST

val q2 = quote {
  query[Person].sortBy(p => p.age)(Ord.descNullsLast)
}

db.run(q2)
// SELECT p.id, p.name, p.age FROM Person p ORDER BY p.age DESC NULLS LAST

val q3 = quote {
  query[Person].sortBy(p => (p.name, p.age))(Ord(Ord.asc, Ord.desc))
}

db.run(q3)
// SELECT p.id, p.name, p.age FROM Person p ORDER BY p.name ASC, p.age DESC
```

**drop/take**

```scala
val q = quote {
  query[Person].drop(2).take(1)
}

db.run(q)
// SELECT x.id, x.name, x.age FROM Person x LIMIT 1 OFFSET 2
```

**groupBy**
```scala
val q = quote {
  query[Person].groupBy(p => p.age).map {
    case (age, people) =>
      (age, people.size)
  }
}

db.run(q)
// SELECT p.age, COUNT(*) FROM Person p GROUP BY p.age
```

**union**
```scala
val q = quote {
  query[Person].filter(p => p.age > 18).union(query[Person].filter(p => p.age > 60))
}

db.run(q)
// SELECT x.id, x.name, x.age FROM (SELECT id, name, age FROM Person p WHERE p.age > 18 
// UNION SELECT id, name, age FROM Person p1 WHERE p1.age > 60) x
```

**unionAll/++**
```scala
val q = quote {
  query[Person].filter(p => p.age > 18).unionAll(query[Person].filter(p => p.age > 60))
}

db.run(q) 
// SELECT x.id, x.name, x.age FROM (SELECT id, name, age FROM Person p WHERE p.age > 18 
// UNION ALL SELECT id, name, age FROM Person p1 WHERE p1.age > 60) x

val q2 = quote {
  query[Person].filter(p => p.age > 18) ++ query[Person].filter(p => p.age > 60)
}

db.run(q2) 
// SELECT x.id, x.name, x.age FROM (SELECT id, name, age FROM Person p WHERE p.age > 18 
// UNION ALL SELECT id, name, age FROM Person p1 WHERE p1.age > 60) x
```

**aggregation**
```scala
val r = quote {
  query[Person].map(p => p.age)
}

db.run(r.min) // SELECT MIN(p.age) FROM Person p
db.run(r.max) // SELECT MAX(p.age) FROM Person p
db.run(r.avg) // SELECT AVG(p.age) FROM Person p
db.run(r.sum) // SELECT SUM(p.age) FROM Person p
db.run(r.size) // SELECT COUNT(p.age) FROM Person p
```

**isEmpty/nonEmpty**
```scala
val q = quote {
  query[Person].filter{ p1 => 
    query[Person].filter(p2 => p2.id != p1.id && p2.age == p1.age).isEmpty
  }
}

db.run(q) 
// SELECT p1.id, p1.name, p1.age FROM Person p1 WHERE 
// NOT EXISTS (SELECT * FROM Person p2 WHERE (p2.id <> p1.id) AND (p2.age = p1.age))

val q2 = quote {
  query[Person].filter{ p1 => 
    query[Person].filter(p2 => p2.id != p1.id && p2.age == p1.age).nonEmpty
  }
}

db.run(q2)
// SELECT p1.id, p1.name, p1.age FROM Person p1 WHERE 
// EXISTS (SELECT * FROM Person p2 WHERE (p2.id <> p1.id) AND (p2.age = p1.age))
```

**contains**
```scala
val q = quote {
  query[Person].filter(p => Set(1, 2).contains(p.id))
}

db.run(q)
// SELECT p.id, p.name, p.age FROM Person p WHERE p.id IN (1, 2)

val q1 = quote { (ids: Set[Int]) =>
  query[Person].filter(p => ids.contains(p.id))
}

db.run(q1)
// SELECT p.id, p.name, p.age FROM Person p WHERE p.id IN (?)

val peopleWithContacts = quote {
  query[Person].filter(p => query[Contact].filter(c => c.personId == p.id).nonEmpty)
}
val q2 = quote {
  query[Person].filter(p => peopleWithContacts.contains(p.id))
}

db.run(q2)
// SELECT p.id, p.name, p.age FROM Person p WHERE p.id IN (SELECT p1.* FROM Person p1 WHERE EXISTS (SELECT c.* FROM Contact c WHERE c.personId = p1.id))
```

**distinct**
```scala
val q = quote {
  query[Person].map(p => p.age).distinct
}

db.run(q)
// SELECT DISTINCT p.age FROM Person p
```

**joins**

In addition to applicative joins Quill also supports explicit joins (both inner and left/right/full outer joins).

```scala

val q = quote {
  query[Person].join(query[Contact]).on((p, c) => c.personId == p.id)
}

db.run(q) 
// SELECT p.id, p.name, p.age, c.personId, c.phone•
// FROM Person p INNER JOIN Contact c ON c.personId = p.id

val q = quote {
  query[Person].leftJoin(query[Contact]).on((p, c) => c.personId == p.id)
}

db.run(q) 
// SELECT p.id, p.name, p.age, c.personId, c.phone•
// FROM Person p LEFT JOIN Contact c ON c.personId = p.id

```

The example joins above cover the simple case. What do you do when a query requires joining more than 2 tables?

With Quill the following multi-join queries are equivalent, choose according to preference:

```scala

case class Employer(id: Int, personId: Int, name: String)

val qFlat = quote {
  for{
    (p,e) <- query[Person].join(query[Employer]).on(_.id == _.personId)
       c  <- query[Contact].leftJoin(_.personId == p.id)
  } yield(p, e, c)
}

val qNested = quote {
  for{
    ((p,e),c) <-
      query[Person].join(query[Employer]).on(_.id == _.personId)
      .leftJoin(query[Contact]).on(
        _._1.id == _.personId
      )
  } yield(p, e, c)
}

db.run(qFlat) 
db.run(qNested) 
// SELECT p.id, p.name, p.age, e.id, e.personId, e.name, c.id, c.phone•
// FROM Person p INNER JOIN Employer e ON p.id = e.personId LEFT JOIN Contact c ON c.personId = p.id

```

# Query probing #

Query probing is an experimental feature that validates queries against the database at compile time, failing the compilation if it is not valid. The query validation does not alter the database state.

This feature is disabled by default. To enable it, mix the `QueryProbing` trait to the database configuration:

```
lazy val db = source(new MySourceConfig("configKey") with QueryProbing)
```

The config configuration must be self-contained, not having references to variables outside its scope. This allows the macro load the source instance at compile-time.

The configurations correspondent to the config key must be available at compile time. You can achieve it by adding this line to your project settings:

```
unmanagedClasspath in Compile += baseDirectory.value / "src" / "main" / "resources"
```

If your project doesn't have a standard layout, e.g. a play project, you should configure the path to point to the folder that contains your config file. 

# Actions #

Database actions are defined using quotations as well. These actions don't have a collection-like API but rather a custom DSL to express inserts, deletes and updates.

  Note: Actions take either a List (in which case the query is batched) or a single value.

**insert**
```scala
val a = quote(query[Contact].insert)

db.run(a)(List(Contact(999, "+1510488988"))) 
// INSERT INTO Contact (personId,phone) VALUES (?, ?)
db.run(a)(Contact(999, "+1510488988"))
// insert single item
```

It is also possible to insert specific columns:

```scala
val a = quote {
  (personId: Int, phone: String) =>
    query[Contact].insert(_.personId -> personId, _.phone -> phone)
}

db.run(a)(List((999, "+1510488988"))) 
// INSERT INTO Contact (personId,phone) VALUES (?, ?)
```

Or column queries:

```scala
val a = quote {
  (id: Int) =>
    query[Person].insert(_.id -> id, _.age -> query[Person].map(p => p.age).max)
}

db.run(a)(List(999)) 
// INSERT INTO Person (id,age) VALUES (?, (SELECT MAX(p.age) FROM Person p))
```

**update**
```scala
val a = quote {
  query[Person].filter(_.id == 999).update
}

db.run(a)(List(Person(999, "John", 22)))
// UPDATE Person SET id = ?, name = ?, age = ? WHERE id = 999
db.run(a)(Person(999, "John", 22))
// update single item
```

Using specific columns:

```scala
val a = quote {
  (id: Int, age: Int) =>
    query[Person].filter(p => p.id == id).update(_.age -> age)
}

db.run(a)(List((999, 18)))
// UPDATE Person SET age = ? WHERE id = ?
```

Using columns as part of the update:

```scala
val a = quote {
  (id: Int) =>
    query[Person].filter(p => p.id == id).update(p => p.age -> (p.age + 1))
}

db.run(a)(List(999))
// UPDATE Person SET age = (age + 1) WHERE id = ?
```

Using column a query:

```scala
val a = quote {
  (id: Int) =>
    query[Person].filter(p => p.id == id).update(_.age -> query[Person].map(p => p.age).max)
}

db.run(a)(List(999))
// UPDATE Person SET age = (SELECT MAX(p.age) FROM Person p) WHERE id = ?
```

**delete**
```scala
val a = quote {
  query[Person].filter(p => p.name == "").delete
}

db.run(a) 
// DELETE FROM Person WHERE name = ''
```

# Implicit query #

Quill provides implicit conversions from case class companion objects to `query[T]` through an extra import:

```scala
import io.getquill.ImplicitQuery._

val q = quote {
  for {
    p <- Person if(p.id == 999)
    c <- Contact if(c.personId == p.id)
  } yield {
    (p.name, c.phone)
  }
}

db.run(q) 
// SELECT p.name, c.phone FROM Person p, Contact c WHERE (p.id = 999) AND (c.personId = p.id)
```

Note the usage of `Person` and `Contact` instead of `query[Person]` and `query[Contact]`.

# SQL-specific operations #

Some operations are sql-specific and not provided with the generic quotation mechanism. The `io.getquill.sources.sql.ops` package has some implicit classes for this kind of operations:

**like**

```scala
import io.getquill.sources.sql.ops._

val q = quote {
  query[Person].filter(p => p.name like "%John%")
}
db.run(q)
// SELECT p.id, p.name, p.age FROM Person p WHERE p.name like '%John%'
```

# Cassandra-specific operations #

The cql-specific operations are provided by the following import:

```scala
import io.getquill.sources.cassandra.ops._
```

The cassandra package also offers a mirror source:

```scala
import io.getquill._

lazy val db = source(new CassandraMirrorSourceConfig("testSource"))
```

Supported operations:

**allowFiltering**

```scala
val q = quote {
  query[Person].filter(p => p.age > 10).allowFiltering
}
db.run(q)
// SELECT id, name, age FROM Person WHERE age > 10 ALLOW FILTERING
```

**ifNotExists**
```scala
val q = quote {
  query[Person].insert(_.age -> 10, _.name -> "John").ifNotExists
}
db.run(q)
// INSERT INTO Person (age,name) VALUES (10, 'John') IF NOT EXISTS
```

**ifExists**
```scala
val q = quote {
  query[Person].filter(p => p.name == "John").delete.ifExists
}
db.run(q)
// DELETE FROM Person WHERE name = 'John' IF EXISTS
```

**usingTimestamp**
```scala
val q1 = quote {
  query[Person].insert(_.age -> 10, _.name -> "John").usingTimestamp(99)
}
db.run(q1)
// INSERT INTO Person (age,name) VALUES (10, 'John') USING TIMESTAMP 99

val q2 = quote {
  query[Person].usingTimestamp(99).update(_.age -> 10)
}
db.run(q2)
// UPDATE Person USING TIMESTAMP 99 SET age = 10
```

**usingTtl**
```scala
val q1 = quote {
  query[Person].insert(_.age -> 10, _.name -> "John").usingTtl(11)
}
db.run(q1)
// INSERT INTO Person (age,name) VALUES (10, 'John') USING TTL 11

val q2 = quote {
  query[Person].usingTtl(11).update(_.age -> 10)
}
db.run(q2)
// UPDATE Person USING TTL 11 SET age = 10

val q3 = quote {
  query[Person].usingTtl(11).filter(_.name == "John").delete
}
db.run(q3)  
// DELETE FROM Person USING TTL 11 WHERE name = 'John'
```

**using**
```scala
val q1 = quote {
  query[Person].insert(_.age -> 10, _.name -> "John").using(ts = 99, ttl = 11)
}
db.run(q1)
// INSERT INTO Person (age,name) VALUES (10, 'John') USING TIMESTAMP 99 AND TTL 11

val q2 = quote {
  query[Person].using(ts = 99, ttl = 11).update(_.age -> 10)
}
db.run(q2)
// UPDATE Person USING TIMESTAMP 99 AND TTL 11 SET age = 10

val q3 = quote {
  query[Person].using(ts = 99, ttl = 11).filter(_.name == "John").delete
}
db.run(q3)
// DELETE FROM Person USING TIMESTAMP 99 AND TTL 11 WHERE name = 'John'
```

**ifCond**
```scala
val q1 = quote {
  query[Person].update(_.age -> 10).ifCond(_.name == "John")
}
db.run(q1)
// UPDATE Person SET age = 10 IF name = 'John'

val q2 = quote {
  query[Person].filter(_.name == "John").delete.ifCond(_.age == 10)
}
db.run(q2)
// DELETE FROM Person WHERE name = 'John' IF age = 10
```

**delete column**
```scala
val q = quote {
  query[Person].map(p => p.age).delete
}
db.run(q)
// DELETE p.age FROM Person
```

# Dynamic queries #

Quill's default operation mode is compile-time, but there are queries that have their structure defined only at runtime. Quill automatically falls back to runtime normalization and query generation if the query's structure is not static. Example:

```scala
import io.getquill._

lazy val db = source(new MirrorSourceConfig("testSource"))

sealed trait QueryType
case object Minor extends QueryType
case object Senior extends QueryType

def people(t: QueryType): Quoted[Query[Person]] =
  t match {
    case Minor => quote {
      query[Person].filter(p => p.age < 18)
    }
    case Senior => quote {
      query[Person].filter(p => p.age > 65)
    }
  }

db.run(people(Minor)) 
// SELECT p.id, p.name, p.age FROM Person p WHERE p.age < 18

db.run(people(Senior)) 
// SELECT p.id, p.name, p.age FROM Person p WHERE p.age > 65
```

# Extending quill #

## Infix ##

Infix is a very flexible mechanism to use non-supported features without having to use plain queries in the target language. It allows insertion of arbitrary strings within quotations.

For instance, quill doesn't support the `FOR UPDATE` SQL feature. It can still be used through infix and implicit classes:

```scala
implicit class ForUpdate[T](q: Query[T]) {
  def forUpdate = quote(infix"$q FOR UPDATE".as[Query[T]])
}

val a = quote {
  query[Person].filter(p => p.age < 18).forUpdate
}

db.run(a)
// SELECT p.id, p.name, p.age FROM (SELECT * FROM Person p WHERE p.age < 18 FOR UPDATE) p
```

The `forUpdate` quotation can be reused for multiple queries.

The same approach can be used for `RETURNING ID`:

```scala
implicit class ReturningId[T](a: Action[T]) {
  def returningId = quote(infix"$a RETURNING ID".as[Action[T]])
}

val a = quote {
  query[Person].insert(_.name -> "John", _.age -> 21).returningId
}

db.run(a)
// INSERT INTO Person (name,age) VALUES ('John', 21) RETURNING ID
```

A custom database function can also be used through infix:

```scala
val myFunction = quote {
  (i: Int) => infix"MY_FUNCTION($i)".as[Int]
}

val q = quote {
  query[Person].map(p => myFunction(p.age))
}

db.run(q) 
// SELECT MY_FUNCTION(p.age) FROM Person p
```

## Custom encoding ##

Quill uses `Encoder`s to encode query inputs and `Decoder`s to read values returned by queries. The library provides a few built-in encodings and two mechanisms to define custom encodings: mapped encoding and raw encoding.

### Mapped Encoding ###

If the correspondent database type is already supported, use `mappedEncoding`. In this example, `String` is already supported by Quill and the `UUID` encoding from/to `String` is defined through mapped encoding:

```scala
import java.util.UUID

implicit val encodeUUID = mappedEncoding[UUID, String](_.toString)
implicit val decodeUUID = mappedEncoding[String, UUID](UUID.fromString(_))
```

### Raw Encoding ###

If the database type is not supported by Quill, it is possible to provide "raw" encoders and decoders:

```
import io.getquill.sources.mirror.Row

implicit val uuidEncoder = 
  db.encoder[UUID] {
    ??? // database-specific implementation
  }

implicit val uuidDecoder = 
  db.decoder[UUID] {
    ??? // database-specific implementation
  }
```

### Wrapped types ###

Quill also supports encoding of "wrapped types". Just extend the `WrappedValue` trait and Quill will automatically encode the underlying primitive type.

```scala
import io.getquill.sources._

case class UserId(value: Int) extends AnyVal with WrappedValue[Int]
case class User(id: UserId, name: String)

val q = quote {
  (id: UserId) => for {
    u <- query[User] if u.id == id
  } yield u
}
db.run(q)(UserId(1))

// SELECT u.id, u.name FROM User u WHERE (u.id = 1)
```

# SQL Sources #

Sources represent the database and provide an execution interface for queries. Example:

```scala
import io.getquill._
import io.getquill.naming.SnakeCase
import io.getquill.sources.sql.idiom.MySQLDialect

lazy val db = source(new JdbcSourceConfig[MySQLDialect, SnakeCase]("db"))
```

## Dialect ##

The SQL dialect to be used by the source is defined by the first type parameter. Some source types are specific to a database and thus not require it.

Quill has three built-in dialects:

- `io.getquill.sources.sql.idiom.H2Dialect`
- `io.getquill.sources.sql.idiom.MySQLDialect`
- `io.getquill.sources.sql.idiom.PostgresDialect`

## Naming strategy ##

The second type parameter defines the naming strategy to be used when translating identifiers (table and column names) to SQL. 


|           strategy                  |          example              |
|-------------------------------------|-------------------------------|
| `io.getquill.naming.Literal`        | some_ident  -> some_ident     |
| `io.getquill.naming.Escape`         | some_ident  -> "some_ident"   |
| `io.getquill.naming.UpperCase`      | some_ident  -> SOME_IDENT     |
| `io.getquill.naming.LowerCase`      | SOME_IDENT  -> some_ident     |
| `io.getquill.naming.SnakeCase`      | someIdent   -> some_ident     |
| `io.getquill.naming.CamelCase`      | some_ident  -> someIdent      |
| `io.getquill.naming.MysqlEscape`    | some_ident  -> \`some_ident\` |
| `io.getquill.naming.PostgresEscape` | $some_ident -> $some_ident    |

Multiple transformations can be defined using mixin. For instance, the naming strategy 

```SnakeCase with UpperCase```

produces the following transformation:

```someIdent -> SOME_IDENT```

The transformations are applied from left to right.

## Configuration ##

The string passed to the source configuration is used as the key to obtain configurations using the [typesafe config](http://github.com/typesafehub/config) library.

Additionally, any member of a source configuration can be overriden. Example:

```
import io.getquill._
import io.getquill.naming.SnakeCase
import io.getquill.sources.sql.idiom.MySQLDialect

lazy val db = source(new JdbcSourceConfig[MySQLDialect, SnakeCase]("db") {
  override def dataSource = ??? // create the datasource manually
})
```

### quill-jdbc ###

Quill uses [HikariCP](https://github.com/brettwooldridge/HikariCP) for connection pooling. Please refer to HikariCP's [documentation](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby) for a detailed explanation of the available configurations.

Note that there are `dataSource` configurations, that go under `dataSource`, like `user` and `password`, but some pool settings may go under the root config, like `connectionTimeout`.

**MySQL**

sbt dependencies
```
libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.36",
  "io.getquill" %% "quill-jdbc" % "0.5.1-SNAPSHOT"
)
```

source definition
```scala
import io.getquill._
import io.getquill.naming.SnakeCase
import io.getquill.sources.sql.idiom.MySQLDialect

lazy val db = source(new JdbcSourceConfig[MySQLDialect, SnakeCase]("db"))
```

application.properties
```
db.dataSourceClassName=com.mysql.jdbc.jdbc2.optional.MysqlDataSource
db.dataSource.url=jdbc:mysql://host/database
db.dataSource.user=root
db.dataSource.password=root
db.dataSource.cachePrepStmts=true
db.dataSource.prepStmtCacheSize=250
db.dataSource.prepStmtCacheSqlLimit=2048
db.connectionTimeout=30000
```

**Postgres**

sbt dependencies
```
libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "9.4-1206-jdbc41",
  "io.getquill" %% "quill-jdbc" % "0.5.1-SNAPSHOT"
)
```

source definition
```scala
import io.getquill._
import io.getquill.naming.SnakeCase
import io.getquill.sources.sql.idiom.PostgresDialect

lazy val db = source(new JdbcSourceConfig[PostgresDialect, SnakeCase]("db"))
```

application.properties
```
db.dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
db.dataSource.user=root
db.dataSource.password=root
db.dataSource.databaseName=database
db.dataSource.portNumber=5432
db.dataSource.serverName=host
db.connectionTimeout=30000
```

### quill-async ###

**MySQL Async**

sbt dependencies
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-async" % "0.5.1-SNAPSHOT"
)
```

source definition
```scala
import io.getquill._
import io.getquill.naming.SnakeCase

lazy val db = source(new MysqlAsyncSourceConfig[SnakeCase]("db"))
```

application.properties
```
db.host=host
db.port=3306
db.user=root
db.password=root
db.database=database
db.poolMaxQueueSize=4
db.poolMaxObjects=4
db.poolMaxIdle=999999999
db.poolValidationInterval=100
```

**Postgres Async**

sbt dependencies
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-async" % "0.5.1-SNAPSHOT"
)
```

source definition
```scala
import io.getquill._
import io.getquill.naming.SnakeCase

lazy val db = source(new PostgresAsyncSourceConfig[SnakeCase]("db"))
```

application.properties
```
db.host=host
db.port=5432
db.user=root
db.password=root
db.database=database
db.poolMaxQueueSize=4
db.poolMaxObjects=4
db.poolMaxIdle=999999999
db.poolValidationInterval=100
```

### quill-finagle-mysql ###

sbt dependencies
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-finagle-mysql" % "0.5.1-SNAPSHOT"
)
```

source definition
```scala
import io.getquill._
import io.getquill.naming.SnakeCase

lazy val db = source(new FinagleMysqlSourceConfig[SnakeCase]("db"))
```

application.properties
```
db.dest=localhost:3306
db.user=root
db.password=root
db.database=database
db.pool.watermark.low=0
db.pool.watermark.high=10
db.pool.idleTime=5 # seconds
db.pool.bufferSize=0
db.pool.maxWaiters=2147483647
```

# Cassandra Sources #

sbt dependencies
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-cassandra" % "0.5.1-SNAPSHOT"
)
```

**synchronous source**
```scala
import io.getquill._
import io.getquill.naming.SnakeCase

lazy val db = source(new CassandraSyncSourceConfig[SnakeCase]("db"))
```

**asynchronous source**
```scala
import io.getquill._
import io.getquill.naming.SnakeCase

lazy val db = source(new CassandraAsyncSourceConfig[SnakeCase]("db"))
```

**stream source**
```scala
import io.getquill._
import io.getquill.naming.SnakeCase

lazy val db = source(new CassandraStreamSourceConfig[SnakeCase]("db"))
```

The configurations are set using runtime reflection on the [`Cluster.builder`](https://docs.datastax.com/en/drivers/java/2.1/com/datastax/driver/core/Cluster.Builder.html) instance. It is possible to set nested structures like `queryOptions.consistencyLevel`, use enum values like `LOCAL_QUORUM`, and set multiple parameters like in `credentials`.

application.properties
```
db.keyspace=quill_test
db.preparedStatementCacheSize=1000
db.session.contactPoint=127.0.0.1
db.session.queryOptions.consistencyLevel=LOCAL_QUORUM
db.session.withoutMetrics=true
db.session.withoutJMXReporting=false
db.session.credentials.0=root
db.session.credentials.1=pass
db.session.maxSchemaAgreementWaitSeconds=1
db.session.addressTranslater=com.datastax.driver.core.policies.IdentityTranslater
```

# Templates #

In order to quickly start with Quill, we have setup some template projects:

* [Play Framework with Quill JDBC](https://github.com/getquill/play-quill-jdbc)

# Slick comparison #

Please refer to [SLICK.md](https://github.com/getquill/quill/blob/master/SLICK.md) for a detailed comparison between Quill and Slick.

# Cassandra libraries comparison #

Please refer to [CASSANDRA.md](https://github.com/getquill/quill/blob/master/CASSANDRA.md) for a detailed comparison between Quill and other main alternatives for interaction with Cassandra in Scala.

# Maintainers #

- @fwbrasil
- @godenji
- @gustavoamigo
- @jilen
- @lvicentesanchez

You can notify all maintainers using the handle `@getquill/maintainers`.

# Acknowledgments #

The project was created having Philip Wadler's talk ["A practical theory of language-integrated query"](http://www.infoq.com/presentations/theory-language-integrated-query) as its initial inspiration. The development was heavily influenced by the following papers:

* [A Practical Theory of Language-Integrated Query](http://homepages.inf.ed.ac.uk/slindley/papers/practical-theory-of-linq.pdf)
* [Everything old is new again: Quoted Domain Specific Languages](http://homepages.inf.ed.ac.uk/wadler/papers/qdsl/qdsl.pdf)
* [The Flatter, the Better](http://db.inf.uni-tuebingen.de/staticfiles/publications/the-flatter-the-better.pdf)

# Code of Conduct #

Please note that this project is released with a Contributor Code of Conduct. By participating in this project you agree to abide by its terms. See [CODE_OF_CONDUCT.md](https://github.com/getquill/quill/blob/master/CODE_OF_CONDUCT.md) for details.

# License #

See the [LICENSE](https://github.com/getquill/quill/blob/master/LICENSE.txt) file for details.
