package io.getquill.context.jdbc

import java.sql.Types
import io.getquill._
import io.getquill.context.ExecutionInfo
import io.getquill.util.ContextLogger

trait PostgresJdbcTypes[N <: NamingStrategy] extends JdbcContextTypes[PostgresDialect, N]
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

trait H2JdbcTypes[N <: NamingStrategy] extends JdbcContextTypes[H2Dialect, N]
  with BooleanObjectEncoding
  with UUIDObjectEncoding {

  val idiom = H2Dialect
}

trait MysqlJdbcTypes[N <: NamingStrategy] extends JdbcContextTypes[MySQLDialect, N]
  with BooleanObjectEncoding
  with UUIDStringEncoding {

  val idiom = MySQLDialect
}

trait SqliteJdbcTypes[N <: NamingStrategy] extends JdbcContextTypes[SqliteDialect, N]
  with BooleanObjectEncoding
  with UUIDObjectEncoding {

  val idiom = SqliteDialect
}

trait SqlServerExecuteOverride[N <: NamingStrategy] extends JdbcContextVerbExecute[SQLServerDialect, N] {

  private val logger = ContextLogger(classOf[SqlServerExecuteOverride[_]])

  override def executeActionReturningMany[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction)(info: ExecutionInfo, dc: Runner): Result[List[O]] =
    withConnectionWrapped { conn =>
      val (params, ps) = prepare(prepareWithReturning(sql, conn, returningBehavior), conn)
      logger.logQuery(sql, params)
      extractResult(ps.executeQuery, conn, extractor)
    }
}

trait SqlServerJdbcTypes[N <: NamingStrategy] extends JdbcContextTypes[SQLServerDialect, N]
  with BooleanObjectEncoding
  with UUIDStringEncoding {

  val idiom = SQLServerDialect
}

trait OracleJdbcTypes[N <: NamingStrategy] extends JdbcContextTypes[OracleDialect, N]
  with BooleanIntEncoding
  with UUIDStringEncoding {

  val idiom = OracleDialect
}
