package io.getquill.context.jdbc

import io.getquill._

trait PostgresJdbcContextBase[+D <: PostgresDialect, +N <: NamingStrategy]
    extends PostgresJdbcTypes[D, N]
    with JdbcContextBase[D, N]

trait H2JdbcContextBase[+D <: H2Dialect, +N <: NamingStrategy] extends H2JdbcTypes[D, N] with JdbcContextBase[D, N]

trait MysqlJdbcContextBase[+D <: MySQLDialect, +N <: NamingStrategy]
    extends MysqlJdbcTypes[D, N]
    with JdbcContextBase[D, N]

trait SqliteJdbcContextBase[+D <: SqliteDialect, +N <: NamingStrategy]
    extends SqliteJdbcTypes[D, N]
    with SqliteExecuteOverride[D, N]
    with JdbcContextBase[D, N]

trait SqlServerJdbcContextBase[+D <: SQLServerDialect, +N <: NamingStrategy]
    extends SqlServerJdbcTypes[D, N]
    with SqlServerExecuteOverride[N]
    with JdbcContextBase[D, N]

trait OracleJdbcContextBase[+D <: OracleDialect, +N <: NamingStrategy]
    extends OracleJdbcTypes[D, N]
    with JdbcContextBase[D, N]
