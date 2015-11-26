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

1. **Boilerplate-free mapping**: The database's schema is mapped using simple case classes.
2. **Quoted DSL**: Queries are defined inside a `quote` block. Quill parses each quoted block of code (quotation) at compile time and translates them to an internal Abstract Syntax Tree (AST)
3. **Compile-time SQL generation**: The `db.run` call reads the quotation's AST and translates it to the target language at compile time, emitting the SQL string as a compilation message. As the query string is known at compile time, the runtime overhead is very low and similar to using the database driver directly.
4. **Compile-time query validation**: If configured, the query is verified against the database at compile time and the compilation fails if it is not valid.

# Sources #

Sources represent the database and provide an execution interface for queries. Example:

```tut
import io.getquill.naming.SnakeCase
import io.getquill.source.jdbc.JdbcSource
import io.getquill.source.sql.idiom.MySQLDialect

object db extends JdbcSource[MySQLDialect, SnakeCase]
```

## Dialect ##

The sql dialect to be used by the source is defined by the first type parameter. Some source types are specific to a database and thus not require the type parameter.

Quill has two built-in dialects:

- `io.getquill.source.sql.idiom.MySQLDialect`
- `io.getquill.source.sql.idiom.PostgresDialect`

## Naming strategy ##

The second type parameter defines the naming strategy to be used when translating identifiers (table and column names) to SQL. 

---------------------------------------------------------------
|           strategy             |          example           |
---------------------------------------------------------------
| `io.getquill.naming.Literal`   | some_ident -> some_ident   |
| `io.getquill.naming.Escape`    | some_ident -> "some_ident" |
| `io.getquill.naming.UpperCase` | some_ident -> SOME_IDENT   |
| `io.getquill.naming.LowerCase` | SOME_IDENT -> some_ident   |
| `io.getquill.naming.SnakeCase` | someIdent -> some_ident    |
| `io.getquill.naming.CamelCase` | some_ident -> someIdent    |
---------------------------------------------------------------

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
```tut
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
```tut
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
```tut
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
```tut
import io.getquill.naming.SnakeCase
import io.getquill.source.async.mysql.PostgresAsyncSource

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
```tut
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

# Queries #

## Quotation ##

## Joins ##


## Probing ##

# Dynamic queries #

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