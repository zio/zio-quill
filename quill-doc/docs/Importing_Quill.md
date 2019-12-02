---
id: Importing_Quill
title: Importing Quill
---

Quill comes in many flavors to suit your specific needs. It integrates into many frameworks including Doobie, Monix, Finagle and others. This array of options can be a bit dizzying so if you are just getting started, try `quill-jdbc`.


## quill-jdbc

The `quill-jdbc` module provides a simple blocking JDBC context for standard use-cases. For transactions, the JDBC connection is kept in a thread-local variable.

Quill uses [HikariCP](https://github.com/brettwooldridge/HikariCP) for connection pooling. Please refer to HikariCP's [documentation](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby) for a detailed explanation of the available configurations.

Note that there are `dataSource` configurations, that go under `dataSource`, like `user` and `password`, but some pool settings may go under the root config, like `connectionTimeout`.

#### transactions

The `JdbcContext` provides thread-local transaction support:

```
ctx.transaction {
  ctx.run(query[Person].delete)
  // other transactional code
}
```

The body of `transaction` can contain calls to other methods and multiple `run` calls since the transaction is propagated through a thread-local.

### MySQL (quill-jdbc)

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "8.0.17",
  "io.getquill" %% "quill-jdbc" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new MysqlJdbcContext(SnakeCase, "ctx")
```

#### application.properties
```
ctx.dataSourceClassName=com.mysql.cj.jdbc.MysqlDataSource
ctx.dataSource.url=jdbc:mysql://host/database
ctx.dataSource.user=root
ctx.dataSource.password=root
ctx.dataSource.cachePrepStmts=true
ctx.dataSource.prepStmtCacheSize=250
ctx.dataSource.prepStmtCacheSqlLimit=2048
ctx.connectionTimeout=30000
```

### Postgres (quill-jdbc)

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.2.8",
  "io.getquill" %% "quill-jdbc" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new PostgresJdbcContext(SnakeCase, "ctx")
```

#### application.properties
```
ctx.dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
ctx.dataSource.user=root
ctx.dataSource.password=root
ctx.dataSource.databaseName=database
ctx.dataSource.portNumber=5432
ctx.dataSource.serverName=host
ctx.connectionTimeout=30000
```

### Sqlite (quill-jdbc)

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "org.xerial" % "sqlite-jdbc" % "3.28.0",
  "io.getquill" %% "quill-jdbc" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new SqliteJdbcContext(SnakeCase, "ctx")
```

#### application.properties
```
ctx.driverClassName=org.sqlite.JDBC
ctx.jdbcUrl=jdbc:sqlite:/path/to/db/file.db
```

### H2 (quill-jdbc)

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.4.199",
  "io.getquill" %% "quill-jdbc" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new H2JdbcContext(SnakeCase, "ctx")
```

#### application.properties
```
ctx.dataSourceClassName=org.h2.jdbcx.JdbcDataSource
ctx.dataSource.url=jdbc:h2:mem:yourdbname
ctx.dataSource.user=sa
```

### SQL Server (quill-jdbc)

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "com.microsoft.sqlserver" % "mssql-jdbc" % "7.4.1.jre8",
  "io.getquill" %% "quill-jdbc" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new SqlServerJdbcContext(SnakeCase, "ctx")
```

### Oracle (quill-jdbc)

Quill supports Oracle version 12c and up although due to licensing restrictions, version 18c XE is used for testing.

Note that the latest Oracle JDBC drivers are not publicly available. In order to get them,
you will need to connect to Oracle's private maven repository as instructed [here](https://docs.oracle.com/middleware/1213/core/MAVEN/config_maven_repo.htm#MAVEN9012).
Unfortunately, this procedure currently does not work for SBT. There are various workarounds
available for this situation [here](https://stackoverflow.com/questions/1074869/find-oracle-jdbc-driver-in-maven-repository?rq=1).

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "com.oracle.jdbc" % "ojdbc8" % "18.3.0.0.0",
  "io.getquill" %% "quill-jdbc" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new OracleJdbcContext(SnakeCase, "ctx")
```


#### application.properties
```
ctx.dataSourceClassName=com.microsoft.sqlserver.jdbc.SQLServerDataSource
ctx.dataSource.user=user
ctx.dataSource.password=YourStrongPassword
ctx.dataSource.databaseName=database
ctx.dataSource.portNumber=1433
ctx.dataSource.serverName=host
```

## quill-jdbc-monix

The `quill-jdbc-monix` module integrates the Monix asynchronous programming framework with Quill,
supporting all of the database vendors of the `quill-jdbc` module. 
The Quill Monix contexts encapsulate JDBC Queries and Actions into Monix `Task`s 
and also include support for streaming queries via `Observable`.

#### streaming

The `MonixJdbcContext` can stream using Monix Observables:

```
ctx.stream(query[Person]) // returns: Observable[Person]
  .foreachL(println(_))
  .runSyncUnsafe()
```

#### transactions

The `MonixJdbcContext` provides support for transactions by storing the connection into a Monix `Local`. 
This process is designed to be completely transparent to the user. As with the other contexts,
if an exception is thrown anywhere inside a task or sub-task within a `transaction` block, the entire block
will be rolled back by the database.

Basic syntax:
```
val trans =
  ctx.transaction {
    for {
      _ <- ctx.run(query[Person].delete)
      _ <- ctx.run(query[Person].insert(Person("Joe", 123)))
      p <- ctx.run(query[Person])
    } yield p
  } //returns: Task[List[Person]]

val result = trans.runSyncUnsafe() //returns: List[Person]
```

Streaming can also be done inside of `transaction` block so long as the result is converted to a task beforehand.
```
val trans =
  ctx.transaction {
    for {
      _   <- ctx.run(query[Person].insert(Person("Joe", 123)))
      ppl <- ctx
              .stream(query[Person])                               // Observable[Person]
              .foldLeftL(List[Person]())({case (l, p) => p +: l})  // ... becomes Task[List[Person]]
    } yield ppl
  } //returns: Task[List[Person]]

val result = trans.runSyncUnsafe() //returns: List[Person]
```

#### runners

Use a `Runner` object to create the different `MonixJdbcContext`s. 
The Runner does the actual wrapping of JDBC calls into Monix Tasks.

```scala

import monix.execution.Scheduler
import io.getquill.context.monix.Runner

// You can use the default Runner when constructing a Monix jdbc contexts. 
// The resulting tasks will be wrapped with whatever Scheduler is 
// defined when you do task.syncRunUnsafe(), typically a global implicit.
lazy val ctx = new MysqlMonixJdbcContext(SnakeCase, "ctx", Runner.default)

// However...
// Monix strongly suggests that you use a separate thread pool for database IO 
// operations. `Runner` provides a convenience method in order to do this.
lazy val ctx = new MysqlMonixJdbcContext(SnakeCase, "ctx", Runner.using(Scheduler.io()))
```

### MySQL (quill-jdbc-monix)

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "8.0.17",
  "io.getquill" %% "quill-jdbc-monix" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new MysqlMonixJdbcContext(SnakeCase, "ctx", Runner.default)
```

#### application.properties
```
ctx.dataSourceClassName=com.mysql.cj.jdbc.MysqlDataSource
ctx.dataSource.url=jdbc:mysql://host/database
ctx.dataSource.user=root
ctx.dataSource.password=root
ctx.dataSource.cachePrepStmts=true
ctx.dataSource.prepStmtCacheSize=250
ctx.dataSource.prepStmtCacheSqlLimit=2048
ctx.connectionTimeout=30000
```

### Postgres (quill-jdbc-monix)

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.2.8",
  "io.getquill" %% "quill-jdbc-monix" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new PostgresMonixJdbcContext(SnakeCase, "ctx", Runner.default)
```

#### application.properties
```
ctx.dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
ctx.dataSource.user=root
ctx.dataSource.password=root
ctx.dataSource.databaseName=database
ctx.dataSource.portNumber=5432
ctx.dataSource.serverName=host
ctx.connectionTimeout=30000
```

### Sqlite (quill-jdbc-monix)

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "org.xerial" % "sqlite-jdbc" % "3.28.0",
  "io.getquill" %% "quill-jdbc-monix" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new SqliteMonixJdbcContext(SnakeCase, "ctx", Runner.default)
```

#### application.properties
```
ctx.driverClassName=org.sqlite.JDBC
ctx.jdbcUrl=jdbc:sqlite:/path/to/db/file.db
```

### H2 (quill-jdbc-monix)

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.4.199",
  "io.getquill" %% "quill-jdbc-monix" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new H2MonixJdbcContext(SnakeCase, "ctx", Runner.default)
```

#### application.properties
```
ctx.dataSourceClassName=org.h2.jdbcx.JdbcDataSource
ctx.dataSource.url=jdbc:h2:mem:yourdbname
ctx.dataSource.user=sa
```

### SQL Server (quill-jdbc-monix)

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "com.microsoft.sqlserver" % "mssql-jdbc" % "7.4.1.jre8",
  "io.getquill" %% "quill-jdbc-monix" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new SqlServerMonixJdbcContext(SnakeCase, "ctx", Runner.default)
```

#### application.properties
```
ctx.dataSourceClassName=com.microsoft.sqlserver.jdbc.SQLServerDataSource
ctx.dataSource.user=user
ctx.dataSource.password=YourStrongPassword
ctx.dataSource.databaseName=database
ctx.dataSource.portNumber=1433
ctx.dataSource.serverName=host
```

### Oracle (quill-jdbc-monix)

Quill supports Oracle version 12c and up although due to licensing restrictions, version 18c XE is used for testing.

Note that the latest Oracle JDBC drivers are not publicly available. In order to get them,
you will need to connect to Oracle's private maven repository as instructed [here](https://docs.oracle.com/middleware/1213/core/MAVEN/config_maven_repo.htm#MAVEN9012).
Unfortunately, this procedure currently does not work for SBT. There are various workarounds
available for this situation [here](https://stackoverflow.com/questions/1074869/find-oracle-jdbc-driver-in-maven-repository?rq=1).

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "com.oracle.jdbc" % "ojdbc8" % "18.3.0.0.0",
  "io.getquill" %% "quill-jdbc-monix" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new OracleJdbcContext(SnakeCase, "ctx")
```

#### application.properties
```
ctx.dataSourceClassName=oracle.jdbc.xa.client.OracleXADataSource
ctx.dataSource.databaseName=xe
ctx.dataSource.user=database
ctx.dataSource.password=YourStrongPassword
ctx.dataSource.driverType=thin
ctx.dataSource.portNumber=1521
ctx.dataSource.serverName=host
```

## NDBC Context

Async support via [NDBC driver](https://ndbc.io/) is available with Postgres database.

### quill-ndbc-postgres

#### transactions

Transaction support is provided out of the box by NDBC:

```scala
ctx.transaction {
  ctx.run(query[Person].delete)
  // other transactional code
}
```

The body of transaction can contain calls to other methods and multiple run calls since the transaction is automatically handled.

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-ndbc-postgres" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new NdbcPostgresContext(Literal, "ctx")
```

#### application.properties
```
ctx.ndbc.dataSourceSupplierClass=io.trane.ndbc.postgres.netty4.DataSourceSupplier
ctx.ndbc.host=host
ctx.ndbc.port=1234
ctx.ndbc.user=root
ctx.ndbc.password=root
ctx.ndbc.database=database
```

## quill-async

The `quill-async` module provides simple async support for MySQL and Postgres databases.

#### transactions

The async module provides transaction support based on a custom implicit execution context:

```
ctx.transaction { implicit ec =>
  ctx.run(query[Person].delete)
  // other transactional code
}
```

The body of `transaction` can contain calls to other methods and multiple `run` calls, but the transactional code must be done using the provided implicit execution context. For instance:

```
def deletePerson(name: String)(implicit ec: ExecutionContext) = 
  ctx.run(query[Person].filter(_.name == lift(name)).delete)

ctx.transaction { implicit ec =>
  deletePerson("John")
}
```

Depending on how the main execution context is imported, it is possible to produce an ambiguous implicit resolution. A way to solve this problem is shadowing the multiple implicits by using the same name:

```
import scala.concurrent.ExecutionContext.Implicits.{ global => ec }

def deletePerson(name: String)(implicit ec: ExecutionContext) = 
  ctx.run(query[Person].filter(_.name == lift(name)).delete)

ctx.transaction { implicit ec =>
  deletePerson("John")
}
```

Note that the global execution context is renamed to ec.

#### application.properties

##### connection configuration
```
ctx.host=host
ctx.port=1234
ctx.user=root
ctx.password=root
ctx.database=database
```

or use connection URL with database-specific scheme (see below):

```
ctx.url=scheme://host:5432/database?user=root&password=root
```

##### connection pool configuration
```
ctx.poolMaxQueueSize=4
ctx.poolMaxObjects=4
ctx.poolMaxIdle=999999999
ctx.poolValidationInterval=10000
```

Also see [`PoolConfiguration` documentation](https://github.com/mauricio/postgresql-async/blob/master/db-async-common/src/main/scala/com/github/mauricio/async/db/pool/PoolConfiguration.scala).

##### SSL configuration
```
ctx.sslmode=disable # optional, one of [disable|prefer|require|verify-ca|verify-full]
ctx.sslrootcert=./path/to/cert/file # optional, required for sslmode=verify-ca or verify-full
```

##### other
```
ctx.charset=UTF-8
ctx.maximumMessageSize=16777216
ctx.connectTimeout=5s
ctx.testTimeout=5s
ctx.queryTimeout=10m
```

### quill-async-mysql

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-async-mysql" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new MysqlAsyncContext(SnakeCase, "ctx")
```

#### application.properties

See [above](#applicationproperties-5)

For `url` property use `mysql` scheme:

```
ctx.url=mysql://host:3306/database?user=root&password=root
```

### quill-async-postgres

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-async-postgres" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new PostgresAsyncContext(SnakeCase, "ctx")
```

#### application.properties

See [common properties](#applicationproperties-5)

For `url` property use `postgresql` scheme:

```
ctx.url=postgresql://host:5432/database?user=root&password=root
```

## Finagle Contexts

Support for the Twitter Finagle library is available with MySQL and Postgres databases.

### quill-finagle-mysql

#### transactions

The finagle context provides transaction support through a `Local` value. See twitter util's [scaladoc](https://github.com/twitter/util/blob/ee8d3140ba0ecc16b54591bd9d8961c11b999c0d/util-core/src/main/scala/com/twitter/util/Local.scala#L96) for more details.

```
ctx.transaction {
  ctx.run(query[Person].delete)
  // other transactional code
}
```

The body of `transaction` can contain calls to other methods and multiple `run` calls since the transaction is automatically propagated through the `Local` value.

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-finagle-mysql" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new FinagleMysqlContext(SnakeCase, "ctx")
```

#### application.properties
```
ctx.dest=localhost:3306
ctx.user=root
ctx.password=root
ctx.database=database
ctx.pool.watermark.low=0
ctx.pool.watermark.high=10
ctx.pool.idleTime=5 # seconds
ctx.pool.bufferSize=0
ctx.pool.maxWaiters=2147483647
```

### quill-finagle-postgres

#### transactions

The finagle context provides transaction support through a `Local` value. See twitter util's [scaladoc](https://github.com/twitter/util/blob/ee8d3140ba0ecc16b54591bd9d8961c11b999c0d/util-core/src/main/scala/com/twitter/util/Local.scala#L96) for more details.

```
ctx.transaction {
  ctx.run(query[Person].delete)
  // other transactional code
}
```

The body of `transaction` can contain calls to other methods and multiple `run` calls since the transaction is automatically propagated through the `Local` value.

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-finagle-postgres" % "3.5.1-SNAPSHOT"
)
```

#### context definition
```scala
lazy val ctx = new FinaglePostgresContext(SnakeCase, "ctx")
```

#### application.properties
```
ctx.host=localhost:3306
ctx.user=root
ctx.password=root
ctx.database=database
ctx.useSsl=false
ctx.hostConnectionLimit=1
ctx.numRetries=4
ctx.binaryResults=false
ctx.binaryParams=false
```

## quill-cassandra

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-cassandra" % "3.5.1-SNAPSHOT"
)
```

#### synchronous context
```scala
lazy val ctx = new CassandraSyncContext(SnakeCase, "ctx")
```

#### asynchronous context
```scala
lazy val ctx = new CassandraAsyncContext(SnakeCase, "ctx")
```

The configurations are set using runtime reflection on the [`Cluster.builder`](https://docs.datastax.com/en/drivers/java/2.1/com/datastax/driver/core/Cluster.Builder.html) instance. It is possible to set nested structures like `queryOptions.consistencyLevel`, use enum values like `LOCAL_QUORUM`, and set multiple parameters like in `credentials`.

#### application.properties
```
ctx.keyspace=quill_test
ctx.preparedStatementCacheSize=1000
ctx.session.contactPoint=127.0.0.1
ctx.session.withPort=9042
ctx.session.queryOptions.consistencyLevel=LOCAL_QUORUM
ctx.session.withoutMetrics=true
ctx.session.withoutJMXReporting=false
ctx.session.credentials.0=root
ctx.session.credentials.1=pass
ctx.session.maxSchemaAgreementWaitSeconds=1
ctx.session.addressTranslator=com.datastax.driver.core.policies.IdentityTranslator
```

## quill-cassandra-monix

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-cassandra-monix" % "3.5.1-SNAPSHOT"
)
```

#### monix context
```scala
lazy val ctx = new CassandraMonixContext(SnakeCase, "ctx")
```

#### stream context
```scala
lazy val ctx = new CassandraStreamContext(SnakeCase, "ctx")
```

## OrientDB Contexts

#### sbt dependencies
```
libraryDependencies ++= Seq(
  "io.getquill" %% "quill-orientdb" % "3.5.1-SNAPSHOT"
)
```

#### synchronous context
```scala
lazy val ctx = new OrientDBSyncContext(SnakeCase, "ctx")
```

The configurations are set using [`OPartitionedDatabasePool`](http://orientdb.com/javadoc/latest/com/orientechnologies/orient/core/db/OPartitionedDatabasePool.html) which creates a pool of DB connections from which an instance of connection can be acquired. It is possible to set DB credentials using the parameter called `username` and `password`.

#### application.properties
```
ctx.dbUrl=remote:127.0.0.1:2424/GratefulDeadConcerts
ctx.username=root
ctx.password=root
```