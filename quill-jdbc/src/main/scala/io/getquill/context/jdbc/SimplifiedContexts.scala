package io.getquill.context.jdbc

import java.sql.Types
import io.getquill._
import io.getquill.context.ExecutionInfo

trait PostgresJdbcContextSimplified[N <: NamingStrategy] extends JdbcContextSimplified[PostgresDialect, N]
  with PostgresJdbcComposition[N] with JdbcRunContext[PostgresDialect, N]

trait PostgresJdbcComposition[N <: NamingStrategy] extends JdbcComposition[PostgresDialect, N]
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
  with H2JdbcComposition[N] with JdbcRunContext[H2Dialect, N]

trait H2JdbcComposition[N <: NamingStrategy] extends JdbcComposition[H2Dialect, N]
  with BooleanObjectEncoding
  with UUIDObjectEncoding {

  val idiom = H2Dialect
}

trait MysqlJdbcContextSimplified[N <: NamingStrategy] extends JdbcContextSimplified[MySQLDialect, N]
  with MysqlJdbcComposition[N] with JdbcRunContext[MySQLDialect, N]

trait MysqlJdbcComposition[N <: NamingStrategy] extends JdbcComposition[MySQLDialect, N]
  with BooleanObjectEncoding
  with UUIDStringEncoding {

  val idiom = MySQLDialect
}

trait SqliteJdbcContextSimplified[N <: NamingStrategy] extends JdbcContextSimplified[SqliteDialect, N]
  with SqliteJdbcComposition[N] with JdbcRunContext[SqliteDialect, N]

trait SqliteJdbcComposition[N <: NamingStrategy] extends JdbcComposition[SqliteDialect, N]
  with BooleanObjectEncoding
  with UUIDObjectEncoding {

  val idiom = SqliteDialect
}

trait SqlServerJdbcContextSimplified[N <: NamingStrategy] extends JdbcContextSimplified[SQLServerDialect, N]
  with SqlServerJdbcComposition[N]
  with JdbcRunContext[SQLServerDialect, N]
  with SqlServerExecuteOverride[N]

trait SqlServerExecuteOverride[N <: NamingStrategy] {
  this: JdbcRunContext[SQLServerDialect, N] =>

  override def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction)(executionInfo: ExecutionInfo, dc: DatasourceContext): Result[O] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(prepareWithReturning(sql, conn, returningBehavior), conn)
      logger.logQuery(sql, params)
      handleSingleResult(extractResult(ps.executeQuery, conn, extractor))
    }
}

trait SqlServerJdbcComposition[N <: NamingStrategy] extends JdbcComposition[SQLServerDialect, N]
  with BooleanObjectEncoding
  with UUIDStringEncoding {

  val idiom = SQLServerDialect
}

trait OracleJdbcContextSimplified[N <: NamingStrategy] extends JdbcContextSimplified[OracleDialect, N]
  with OracleJdbcComposition[N] with JdbcRunContext[OracleDialect, N]

trait OracleJdbcComposition[N <: NamingStrategy] extends JdbcComposition[OracleDialect, N]
  with BooleanIntEncoding
  with UUIDStringEncoding {

  val idiom = OracleDialect
}
