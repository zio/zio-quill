![quill](https://raw.githubusercontent.com/getquill/quill/master/quill.png)
# Quill
Compile-time Language Integrated Query for Scala

[![Build Status](https://img.shields.io/travis/getquill/quill.svg)](https://api.travis-ci.org/getquill/quill.svg?branch=master)
[![Codacy Badge](https://img.shields.io/codacy/36ab84c7ff43480489df9b7312a4bdc1.svg)](https://www.codacy.com/app/fwbrasil/quill)
[![codecov.io](https://img.shields.io/codecov/c/github/getquill/quill.svg)](http://codecov.io/github/getquill/quill?branch=master)
[![Join the chat at https://gitter.im/getquill/quill](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/getquill/quill?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![maven](https://img.shields.io/maven-central/v/io.getquill/quill_2.11.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.getquill%22)

# Overview #

Quill provides a Quoted Domain Specific Language (QDSL) to express queries in Scala and execute them in a target language. The library's core is designed to support multiple target languages but the current version only supports the generation of Structured Language Queries (SQL) for interaction with relational databases.

![example](https://raw.githubusercontent.com/getquill/quill/master/example.gif)

1. **Boilerplate-free mapping**: The database schema is mapped using simple case classes.
2. **Quoted DSL**: Queries are defined inside a `quote` block. Quill parses each quoted block of code (quotation) at compile time and translates them to an internal Abstract Syntax Tree (AST)
3. **Compile-time SQL generation**: The `db.run` call reads the quotation's AST and translates it to the target language at compile time, emitting the SQL string as a compilation message. As the query string is known at compile time, the runtime overhead is very low and similar to using the database driver directly.
4. **Compile-time query validation**: If configured, the query is verified against the database at compile time and the compilation fails if it is not valid.

# Sources #

Sources represent the database and provide an execution interface for queries. Example:

```scala
import io.getquill.naming.SnakeCase
import io.getquill.source.jdbc.JdbcSource
import io.getquill.source.sql.idiom.MySQLDialect

object db extends JdbcSource[MySQLDialect, SnakeCase]
```

## Dialect ##

The sql dialect to be used by the source is defined by the first type parameter. Some source types are specific to a database and thus not require it.

Quill has two built-in dialects:

- `io.getquill.source.sql.idiom.MySQLDialect`
- `io.getquill.source.sql.idiom.PostgresDialect`

## Naming strategy ##

The second type parameter defines the naming strategy to be used when translating identifiers (table and column names) to SQL. 


|           strategy             |          example           |
|--------------------------------|----------------------------|
| `io.getquill.naming.Literal`   | some_ident -> some_ident   |
| `io.getquill.naming.Escape`    | some_ident -> "some_ident" |
| `io.getquill.naming.UpperCase` | some_ident -> SOME_IDENT   |
| `io.getquill.naming.LowerCase` | SOME_IDENT -> some_ident   |
| `io.getquill.naming.SnakeCase` | someIdent -> some_ident    |
| `io.getquill.naming.CamelCase` | some_ident -> someIdent    |


Multiple transformations can be defined using mixin. For instance, the naming strategy 

```SnakeCase with UpperCase```

produces this transformation:

```someIdent -> SOME_IDENT```

The transformations are applied from left to right.

## Configuration ##

Sources must be defined as `object` and the object name is used as the key to obtain configurations using the [typesafe config](http://github.com/typesafehub/config) library.

### quill-jdbc ###

**MySQL**

sbt dependencies
```
libraryDependencies ++= Seq(
	"mysql" % "mysql-connector-java" % "5.1.36",
	"io.getquill" % "quill-jdbc" % "0.1.0"
)
```

source definition
```scala
import io.getquill.naming.SnakeCase
import io.getquill.source.jdbc.JdbcSource
import io.getquill.source.sql.idiom.MySQLDialect

object db extends JdbcSource[MySQLDialect, SnakeCase]
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
```

**Postgres**

sbt dependencies
```
libraryDependencies ++= Seq(
	"postgresql" % "postgresql" % "9.1-901.jdbc4",
	"io.getquill" % "quill-jdbc" % "0.1.0"
)
```

source definition
```scala
import io.getquill.naming.SnakeCase
import io.getquill.source.jdbc.JdbcSource
import io.getquill.source.sql.idiom.PostgresDialect

object db extends JdbcSource[PostgresDialect, SnakeCase]
```

application.properties
```
db.dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
db.dataSource.user=root
db.dataSource.password=root
db.dataSource.databaseName=database
db.dataSource.portNumber=5432
db.dataSource.serverName=host
```

Please refer to HikariCP's [documentation](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby) for a detailed explanation of the available configurations.

### quill-async ###

**MySQL Async**

sbt dependencies
```
libraryDependencies ++= Seq(
	"io.getquill" % "quill-async" % "0.1.0"
)
```

source definition
```scala
import io.getquill.naming.SnakeCase
import io.getquill.source.async.mysql.MysqlAsyncSource

object db extends MysqlAsyncSource[SnakeCase]
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
	"io.getquill" % "quill-async" % "0.1.0"
)
```

source definition
```scala
import io.getquill.naming.SnakeCase
import io.getquill.source.async.postgres.PostgresAsyncSource

object db extends PostgresAsyncSource[SnakeCase]
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
	"io.getquill" % "quill-finagle-mysql" % "0.1.0"
)
```

source definition
```scala
import io.getquill.naming.SnakeCase
import io.getquill.source.finagle.mysql.FinagleMysqlSource

object db extends FinagleMysqlSource[SnakeCase]
```

application.properties
```
testDB.dest=localhost:3306
testDB.user=root
testDB.password=root
testDB.database=database
```

# Mirror sources #

Quill provides mirror sources for test purposes. Instead of running the query, they return a mirror with the information that would be used to run the query. There are two mirror source versions:

- `io.getquill.source.mirror.mirrorSource`: Mirrors the quotation ast
- `io.getquill.source.sql.mirror.mirrorSource`: Mirrors the sql query

This documentation uses the sql mirror in its examples under the `db` name:

```scala
val db = io.getquill.source.sql.mirror.mirrorSource
```

# Quotation #

The QDSL allows the user to write plain scala code, leveraging scala's syntax and type system. Quotations are created using the `quote` method and can contain any excerpt of code that uses supported operations. To create quotations, first import `quote` and some other auxiliary methods:

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

Quotations can also contain high-order functions:

```scala
val area = quote {
  (c: Circle) => pi * c.radius * c.radius
}
```

```scala
val areas = quote {
  query[Circle].map(c => area(c))
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
  query[Circle].filter(c1 => existsAny(query[Circle])(c2 => c2.radius > c1.radius))
}
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
  query[Circle]("circle_table", _.radius -> "radius_column")
}

val q = quote {
  circles.filter(c => c.radius > 1)
}

db.run(q) // SELECT c.radius_column FROM circle_table c WHERE c.radius_column > 1
```

If multiple tables require custom identifiers, it is good practice to define a `schema` object with all table queries to be reused across multiple queries:

```scala
object schema {
  case class Circle(radius: Int)
  val circles = quote {
    query[Circle]("circle_table", _.radius -> "radius_column")
  }
  case class Rectangle(length: Int, width: Int)
  val rectangles = quote {
    query[Rectangle]("rectangle_table", _.length -> "length_column", _.width -> "width_column")
  }
}
```

# Queries #

The overall abstraction of quill queries is use database tables as if they were scala collections.

**filter**
```scala
val q = quote {
  query[Circle].filter(c => c.radius > 0)
}

db.run(q) // SELECT c.radius FROM Circle c where c.radius > 0
```

**map**
```scala
val q = quote {
  query[Circle].map(c => c.radius)
}

db.run(q) // SELECT c.radius FROM Circle
```

**sortBy**
```scala
val q = quote {
  query[Circle].sortBy(c => c.radius)
}

db.run(q) // SELECT c.radius FROM Circle SORT BY c.radius
```

**drop/take**
```scala
val q = quote {
  query[Circle].drop(2).take(1)
}

db.run(q) // SELECT c.radius FROM Circle LIMIT 1 OFFSET 2
```

**union**
```scala
val q = quote {
  query[Circle].drop(2).take(1)
}

db.run(q) // SELECT c.radius FROM Circle LIMIT 1 OFFSET 2
```


## Joins ##


## Probing ##

# Dynamic queries #

# Actions #

# Extending quill #

## Custom encoding ##

## Infix ##

## Custom dialect ##

## Custom naming strategy ##

# Internals #

# Known limitations #

# Acknowledgments #

# Contributing #

# License #

See the [LICENSE](https://github.com/getquill/quill/blob/master/LICENSE.txt) file for details.