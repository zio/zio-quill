package io.getquill.codegen.jdbc

import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

import scala.util.{Failure, Success, Try}

object DatabaseTypes {
  import io.getquill._

  def all: Seq[DatabaseType] = Seq(H2, MySql, SqlServer, Postgres, Sqlite, Oracle)

  object DatabaseType {
    def fromProductName(productName: String): Try[DatabaseType] = {
      val res = all.find(_.databaseName == productName)
      if (res.isDefined) Success(res.get)
      else
        Failure(
          new IllegalArgumentException(
            s"Database type ${productName} not supported. Possible Values are: ${all.map(_.databaseName)}"
          )
        )
    }
  }

  sealed trait DatabaseType {
    def databaseName: String; def context: Class[_ <: JdbcContext[_, _]]; def dialect: Class[_ <: SqlIdiom]
  }
  case object H2 extends DatabaseType {
    def databaseName              = "H2"; def context: Class[_ <: JdbcContext[_, _]] = classOf[H2JdbcContext[_]];
    def dialect: Class[H2Dialect] = classOf[H2Dialect]
  }
  case object MySql extends DatabaseType {
    def databaseName                 = "MySQL"; def context: Class[_ <: JdbcContext[_, _]] = classOf[MysqlJdbcContext[_]];
    def dialect: Class[MySQLDialect] = classOf[MySQLDialect]
  }
  case object SqlServer extends DatabaseType {
    def databaseName                           = "Microsoft SQL Server";
    def context: Class[_ <: JdbcContext[_, _]] = classOf[SqlServerJdbcContext[_]];
    def dialect: Class[SQLServerDialect]       = classOf[SQLServerDialect]
  }
  case object Postgres extends DatabaseType {
    def databaseName                    = "PostgreSQL"; def context: Class[_ <: JdbcContext[_, _]] = classOf[PostgresJdbcContext[_]];
    def dialect: Class[PostgresDialect] = classOf[PostgresDialect]
  }
  case object Sqlite extends DatabaseType {
    def databaseName                  = "SQLite"; def context: Class[_ <: JdbcContext[_, _]] = classOf[SqliteJdbcContext[_]];
    def dialect: Class[SqliteDialect] = classOf[SqliteDialect]
  }
  case object Oracle extends DatabaseType {
    def databaseName                  = "Oracle"; def context: Class[_ <: JdbcContext[_, _]] = classOf[SqliteJdbcContext[_]];
    def dialect: Class[OracleDialect] = classOf[OracleDialect]
  }
}
