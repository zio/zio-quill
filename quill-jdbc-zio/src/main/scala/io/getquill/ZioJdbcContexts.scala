package io.getquill

import com.typesafe.config.Config
import io.getquill.context.jdbc.PostgresJdbcRunContext
import io.getquill.context.jdbc.SqlServerJdbcRunContext
import io.getquill.context.jdbc.H2JdbcRunContext
import io.getquill.context.jdbc.MysqlJdbcRunContext
import io.getquill.context.jdbc.SqliteJdbcRunContext
import io.getquill.context.jdbc.OracleJdbcRunContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.qzio.ZioJdbcContext
import io.getquill.util.LoadConfig

import javax.sql.DataSource

class PostgresZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[PostgresDialect, N]
  with PostgresJdbcRunContext[N]

class SqlServerZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[SQLServerDialect, N]
  with SqlServerJdbcRunContext[N]

class H2ZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[H2Dialect, N]
  with H2JdbcRunContext[N]

class MysqlZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[MySQLDialect, N]
  with MysqlJdbcRunContext[N]

class SqliteZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[SqliteDialect, N]
  with SqliteJdbcRunContext[N]

class OracleZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[OracleDialect, N]
  with OracleJdbcRunContext[N]

trait WithProbing[D <: SqlIdiom, N <: NamingStrategy] extends ZioJdbcContext[D, N] {
  def probingConfig: Config;
  override def probingDataSource: Option[DataSource] = Some(JdbcContextConfig(probingConfig).dataSource)
}

trait WithProbingPrefix[D <: SqlIdiom, N <: NamingStrategy] extends WithProbing[D, N] {
  def configPrefix: String;
  def probingConfig: Config = LoadConfig(configPrefix)
}
