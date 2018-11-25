package io.getquill.context.jdbc

import java.sql.Types

import io.getquill._

trait PostgresJdbcContextBase[N <: NamingStrategy] extends JdbcContextBase[PostgresDialect, N]
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

trait H2JdbcContextBase[N <: NamingStrategy] extends JdbcContextBase[H2Dialect, N]
  with UUIDObjectEncoding {

  val idiom = H2Dialect
}

trait MysqlJdbcContextBase[N <: NamingStrategy] extends JdbcContextBase[MySQLDialect, N]
  with UUIDStringEncoding {

  val idiom = MySQLDialect
}

trait SqliteJdbcContextBase[N <: NamingStrategy] extends JdbcContextBase[SqliteDialect, N]
  with UUIDObjectEncoding {

  val idiom = SqliteDialect
}

trait SqlServerJdbcContextBase[N <: NamingStrategy] extends JdbcContextBase[SQLServerDialect, N]
  with UUIDStringEncoding {

  val idiom = SQLServerDialect
}
