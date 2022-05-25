package io.getquill.context.jdbc

import io.getquill._

trait PostgresJdbcContextBase[N <: NamingStrategy]
  extends PostgresJdbcTypes[N]
  with JdbcContextBase[PostgresDialect, N]

trait H2JdbcContextBase[N <: NamingStrategy]
  extends H2JdbcTypes[N]
  with JdbcContextBase[H2Dialect, N]

trait MysqlJdbcContextBase[N <: NamingStrategy]
  extends MysqlJdbcTypes[N]
  with JdbcContextBase[MySQLDialect, N]

trait SqliteJdbcContextBase[N <: NamingStrategy]
  extends SqliteJdbcTypes[N]
  with JdbcContextBase[SqliteDialect, N]

trait SqlServerJdbcContextBase[N <: NamingStrategy]
  extends SqlServerJdbcTypes[N]
  with SqlServerExecuteOverride[N]
  with JdbcContextBase[SQLServerDialect, N]

trait OracleJdbcContextBase[N <: NamingStrategy]
  extends OracleJdbcTypes[N]
  with JdbcContextBase[OracleDialect, N]
