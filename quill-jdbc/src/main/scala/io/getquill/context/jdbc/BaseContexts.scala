package io.getquill.context.jdbc

import io.getquill._

trait PostgresJdbcContextBase[N <: NamingStrategy] extends PostgresJdbcContextSimplified[N] with JdbcContextBase[PostgresDialect, N]

trait H2JdbcContextBase[N <: NamingStrategy] extends H2JdbcContextSimplified[N] with JdbcContextBase[H2Dialect, N]

trait MysqlJdbcContextBase[N <: NamingStrategy] extends MysqlJdbcContextSimplified[N] with JdbcContextBase[MySQLDialect, N]

trait SqliteJdbcContextBase[N <: NamingStrategy] extends SqliteJdbcContextSimplified[N] with JdbcContextBase[SqliteDialect, N]

trait SqlServerJdbcContextBase[N <: NamingStrategy] extends SqlServerJdbcContextSimplified[N] with JdbcContextBase[SQLServerDialect, N]

trait OracleJdbcContextBase[N <: NamingStrategy] extends OracleJdbcContextSimplified[N] with JdbcContextBase[OracleDialect, N]
