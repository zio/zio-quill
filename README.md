![quill](https://raw.githubusercontent.com/getquill/quill/master/quill.png)
# Quill
Compile-time Language Integrated Query for Scala

[![Join the chat at https://gitter.im/getquill/quill](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/getquill/quill?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://img.shields.io/travis/getquill/quill.svg)](https://api.travis-ci.org/getquill/quill.svg?branch=master)
[![Codacy Badge](https://img.shields.io/codacy/36ab84c7ff43480489df9b7312a4bdc1.svg)](https://www.codacy.com/app/fwbrasil/quill)
[![codecov.io](https://img.shields.io/codecov/c/github/getquill/quill.svg)](http://codecov.io/github/getquill/quill?branch=master)

# Overview #

Quill provides a Quoted Domain Specific Language (QDSL) to express queries in Scala and execute them in a target language. The library's core is designed to support multiple target languages but the current version only supports the generation of Structured Language Queries (SQL) for interaction with relational databases.

![example](https://raw.githubusercontent.com/getquill/quill/master/example.gif)

1. **Boilerplate-free mapping**: The database's schema is mapped using simple case classes.
2. **Quoted DSL**: Queries are defined inside a `quote` block. Quill parses each quoted block of code (quotation) at compile time and translates them to an internal Abstract Syntax Tree (AST)
3. **Compile-time SQL generation**: The `db.run` call reads the quotation's AST and translates it to the target language at compile time, emitting the SQL string as a compilation message. As the query string is known at compile time, the runtime overhead is very low and similar to using the database driver directly.
4. **Compile-time query validation**: If configured, the query is verified against the database at compile time and the compilation fails if it is not valid.

# Getting started #

Quill is distributed via maven central. Click on the badge for more information on how to add the dependency to your project:

[![maven](https://img.shields.io/maven-central/v/io.getquill/quill_2.11.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.getquill%22)

The following modules are available:

**Base modules** - used as transitive dependencies

- **quill-core**: the library's core.
- **quill-sql**: especialization of the core module for sql queries.

**Driver-specific modules** - choose one of these as your dependency

- **quill-jdbc**: integrates with jdbc drivers.
- **quill-async**: integrates with [async non-blocking drivers](http://github.com/mauricio/postgresql-async/) for `mysql` and `postgres`
- **quill-finagle-mysql**: uses finagle's async non-blocking [mysql driver](https://github.com/twitter/finagle/tree/develop/finagle-mysql)

# Sources #

Sources represent the database and provide an execution interface for queries. Example:

```tut
import io.getquill.naming.Literal
import io.getquill.source.jdbc.JdbcSource
import io.getquill.source.sql.idiom.MySQLDialect

object db extends JdbcSource[MySQLDialect, Literal]
```

## Dialect ##

The sql dialect to be used by the source is defined by the first type parameter. Some source types are specific to a database and thus not require the type parameter.

Quill has two built-in dialects:

- `io.getquill.source.sql.idiom.MySQLDialect`
- `io.getquill.source.sql.idiom.PostgresDialect`

Please refer to [extending quill](#extending-quill) for information on how to create custom dialects.

## Naming strategy ##

The second type parameter defines the naming strategy to be used when translating table and column names to SQL. 

## Configuration ##

They must be declared as `object`. 

## quill-jdbc ##

```tut
import io.getquill.naming.Literal
import io.getquill.source.jdbc.JdbcSource
import io.getquill.source.sql.idiom.MySQLDialect

object db extends JdbcSource[MySQLDialect, Literal]
```

**application.properties**
```
testDB.dataSourceClassName=com.mysql.jdbc.jdbc2.optional.MysqlDataSource
testDB.dataSource.url=jdbc:mysql://localhost/quill_test
testDB.dataSource.user=root
testDB.dataSource.cachePrepStmts=true
testDB.dataSource.prepStmtCacheSize=250
testDB.dataSource.prepStmtCacheSqlLimit=2048
testDB.maximumPoolSize=4
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