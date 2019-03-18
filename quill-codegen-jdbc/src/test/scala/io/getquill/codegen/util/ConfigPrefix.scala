package io.getquill.codegen.util

import java.io.Closeable
import com.typesafe.config.ConfigFactory
import io.getquill.JdbcContextConfig
import javax.sql.DataSource
import OptionOps._

import scala.util.Try

sealed trait ConfigPrefix {
  def value: String
  def packagePath: String

  private[getquill] def loadConfig =
    ConfigFactory
      .load(getClass.getClassLoader, "application-codegen.conf")
      .getConfig(value)

  private[getquill] def makeDatasource: DataSource with Closeable =
    JdbcContextConfig(loadConfig).dataSource

}

object ConfigPrefix {
  sealed trait TestH2DB extends ConfigPrefix { val value = "testH2DB"; val packagePath = "h2" }
  sealed trait TestMysqlDB extends ConfigPrefix { val value = "testMysqlDB"; val packagePath = "mysql" }
  sealed trait TestOracleDB extends ConfigPrefix { val value = "testOracleDB"; val packagePath = "oracle" }
  sealed trait TestPostgresDB extends ConfigPrefix { val value = "testPostgresDB"; val packagePath = "postgres" }
  sealed trait TestSqliteDB extends ConfigPrefix { val value = "testSqliteDB"; val packagePath = "sqlite" }
  sealed trait TestSqlServerDB extends ConfigPrefix { val value = "testSqlServerDB"; val packagePath = "sqlserver" }

  case object TestH2DB extends TestH2DB
  case object TestMysqlDB extends TestMysqlDB
  case object TestOracleDB extends TestOracleDB
  case object TestPostgresDB extends TestPostgresDB
  case object TestSqliteDB extends TestSqliteDB
  case object TestSqlServerDB extends TestSqlServerDB

  def all = List(
    TestH2DB,
    TestMysqlDB,
    TestOracleDB,
    TestPostgresDB,
    TestSqliteDB,
    TestSqlServerDB
  )

  def fromValue(str: String): Try[ConfigPrefix] =
    ConfigPrefix.all.find(_.value == str).toTry(new IllegalArgumentException(s"Could not find the value: '${str}'"))
}
