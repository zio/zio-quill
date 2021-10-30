package io.getquill

import com.typesafe.config.Config
import io.getquill.context.jdbc.{ H2JdbcComposition, JdbcRunContext, MysqlJdbcComposition, OracleJdbcComposition, PostgresJdbcComposition, SqlServerExecuteOverride, SqlServerJdbcComposition, SqliteJdbcComposition }
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.qzio.{ OuterZioJdbcContext, ZioJdbcContext }
import io.getquill.util.LoadConfig

import javax.sql.DataSource

class OuterPostgresZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends OuterZioJdbcContext[PostgresDialect, N]
  with PostgresJdbcComposition[N] {

  val underlying: ZioJdbcContext[PostgresDialect, N] = new PostgresZioJdbcContext[N](naming)
}

class PostgresZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[PostgresDialect, N]
  with PostgresJdbcComposition[N]
  with JdbcRunContext[PostgresDialect, N]

class OuterSqlServerZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends OuterZioJdbcContext[SQLServerDialect, N]
  with SqlServerJdbcComposition[N] {

  val underlying: ZioJdbcContext[SQLServerDialect, N] = new SqlServerZioJdbcContext[N](naming)
}

class SqlServerZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[SQLServerDialect, N]
  with SqlServerJdbcComposition[N]
  with JdbcRunContext[SQLServerDialect, N]
  with SqlServerExecuteOverride[N]

class OuterH2ZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends OuterZioJdbcContext[H2Dialect, N]
  with H2JdbcComposition[N] {

  val underlying: ZioJdbcContext[H2Dialect, N] = new H2ZioJdbcContext[N](naming)
}

class H2ZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[H2Dialect, N]
  with H2JdbcComposition[N]
  with JdbcRunContext[H2Dialect, N]

class OuterMysqlZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends OuterZioJdbcContext[MySQLDialect, N]
  with MysqlJdbcComposition[N] {

  val underlying: ZioJdbcContext[MySQLDialect, N] = new MysqlZioJdbcContext[N](naming)
}

class MysqlZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[MySQLDialect, N]
  with MysqlJdbcComposition[N]
  with JdbcRunContext[MySQLDialect, N]

class OuterSqliteZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends OuterZioJdbcContext[SqliteDialect, N]
  with SqliteJdbcComposition[N] {

  val underlying: ZioJdbcContext[SqliteDialect, N] = new SqliteZioJdbcContext[N](naming)
}

class SqliteZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[SqliteDialect, N]
  with SqliteJdbcComposition[N]
  with JdbcRunContext[SqliteDialect, N]

class OuterOracleZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends OuterZioJdbcContext[OracleDialect, N]
  with OracleJdbcComposition[N] {

  val underlying: ZioJdbcContext[OracleDialect, N] = new OracleZioJdbcContext[N](naming)
}

class OracleZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[OracleDialect, N]
  with OracleJdbcComposition[N]
  with JdbcRunContext[OracleDialect, N]

trait WithProbing[D <: SqlIdiom, N <: NamingStrategy] extends ZioJdbcContext[D, N] {
  def probingConfig: Config;
  override def probingDataSource: Option[DataSource] = Some(JdbcContextConfig(probingConfig).dataSource)
}

trait WithProbingPrefix[D <: SqlIdiom, N <: NamingStrategy] extends WithProbing[D, N] {
  def configPrefix: String;
  def probingConfig: Config = LoadConfig(configPrefix)
}
