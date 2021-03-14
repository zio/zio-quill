package io.getquill.context.jdbc

import java.sql.Types

import io.getquill._

trait PostgresJdbcContextSimplified[N <: NamingStrategy] extends JdbcContextSimplified[PostgresDialect, N]
  with PostgresJdbcRunContext[N]

trait PostgresJdbcRunContext[N <: NamingStrategy] extends JdbcRunContext[PostgresDialect, N]
  with BooleanObjectEncoding
  with UUIDObjectEncoding
  with ArrayDecoders
  with ArrayEncoders {

  val idiom = PostgresDialect

  override def parseJdbcType(intType: Int): String = intType match {
    case Types.TINYINT => super.parseJdbcType(Types.SMALLINT)
    case Types.VARCHAR => "text"
    case Types.DOUBLE  => "float8"
    case _             => super.parseJdbcType(intType)
  }
}

trait H2JdbcContextSimplified[N <: NamingStrategy] extends JdbcContextSimplified[H2Dialect, N]
  with H2JdbcRunContext[N]

trait H2JdbcRunContext[N <: NamingStrategy] extends JdbcRunContext[H2Dialect, N]
  with BooleanObjectEncoding
  with UUIDObjectEncoding {

  val idiom = H2Dialect
}

trait MysqlJdbcContextSimplified[N <: NamingStrategy] extends JdbcContextSimplified[MySQLDialect, N]
  with MysqlJdbcRunContext[N]

trait MysqlJdbcRunContext[N <: NamingStrategy] extends JdbcRunContext[MySQLDialect, N]
  with BooleanObjectEncoding
  with UUIDStringEncoding {

  val idiom = MySQLDialect
}

trait SqliteJdbcContextSimplified[N <: NamingStrategy] extends JdbcContextSimplified[SqliteDialect, N]
  with SqliteJdbcRunContext[N]

trait SqliteJdbcRunContext[N <: NamingStrategy] extends JdbcRunContext[SqliteDialect, N]
  with BooleanObjectEncoding
  with UUIDObjectEncoding {

  val idiom = SqliteDialect
}

trait SqlServerJdbcContextSimplified[N <: NamingStrategy] extends JdbcContextSimplified[SQLServerDialect, N]
  with SqlServerJdbcRunContext[N]

trait SqlServerJdbcRunContext[N <: NamingStrategy] extends JdbcRunContext[SQLServerDialect, N]
  with BooleanObjectEncoding
  with UUIDStringEncoding {

  val idiom = SQLServerDialect

  override def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction): Result[O] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(prepareWithReturning(sql, conn, returningBehavior))
      logger.logQuery(sql, params)
      handleSingleResult(extractResult(ps.executeQuery, extractor))
    }
}

trait OracleJdbcContextSimplified[N <: NamingStrategy] extends JdbcContextSimplified[OracleDialect, N]
  with OracleJdbcRunContext[N]

trait OracleJdbcRunContext[N <: NamingStrategy] extends JdbcRunContext[OracleDialect, N]
  with BooleanIntEncoding
  with UUIDStringEncoding {

  val idiom = OracleDialect
}
