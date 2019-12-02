---
id: Configuration
title: Configuration
---

The string passed to the context is used as the key in order to obtain configurations using the [typesafe config](http://github.com/typesafehub/config) library.

Additionally, the contexts provide multiple constructors. For instance, with `JdbcContext` it's possible to specify a `DataSource` directly, without using the configuration:

```scala
def createDataSource: javax.sql.DataSource with java.io.Closeable = ???

lazy val ctx = new MysqlJdbcContext(SnakeCase, createDataSource)
```

TODO more info definitely needed!
