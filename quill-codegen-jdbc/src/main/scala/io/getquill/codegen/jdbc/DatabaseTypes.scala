package io.getquill.codegen.jdbc

import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

import scala.util.{ Failure, Success, Try }

object DatabaseTypes {
  import io.getquill._

  def all: Seq[DatabaseType] = Seq(H2, MySql, SqlServer, Postgres, Sqlite, Oracle)

  object DatabaseType {
    def fromProductName(productName: String): Try[DatabaseType] = {
      val res = all.find(_.databaseName == productName)
      if (res.isDefined) Success(res.get)
      else Failure(
        new IllegalArgumentException(
          s"Database type ${productName} not supported. Possible Values are: ${all.map(_.databaseName)}"
        )
      )
    }
  }

  sealed trait DatabaseType { def databaseName: String; def context: Class[_ <: JdbcContext[_, _]]; def dialect: Class[_ <: SqlIdiom] }
  case object H2 extends DatabaseType { def databaseName = "H2"; def context = classOf[H2JdbcContext[_]]; def dialect = classOf[H2Dialect] }
  case object MySql extends DatabaseType { def databaseName = "MySQL"; def context = classOf[MysqlJdbcContext[_]]; def dialect = classOf[MySQLDialect] }
  case object SqlServer extends DatabaseType { def databaseName = "Microsoft SQL Server"; def context = classOf[SqlServerJdbcContext[_]]; def dialect = classOf[SQLServerDialect] }
  case object Postgres extends DatabaseType { def databaseName = "PostgreSQL"; def context = classOf[PostgresJdbcContext[_]]; def dialect = classOf[PostgresDialect] }
  case object Sqlite extends DatabaseType { def databaseName = "SQLite"; def context = classOf[SqliteJdbcContext[_]]; def dialect = classOf[SqliteDialect] }
  case object Oracle extends DatabaseType { def databaseName = "Oracle"; def context = classOf[SqliteJdbcContext[_]]; def dialect = classOf[OracleDialect] }
}
