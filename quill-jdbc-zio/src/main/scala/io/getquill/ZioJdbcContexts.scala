package io.getquill

import com.typesafe.config.Config
import io.getquill.context.jdbc.{ H2JdbcComposition, JdbcRunContext, MysqlJdbcComposition, OracleJdbcComposition, PostgresJdbcComposition, SqlServerExecuteOverride, SqlServerJdbcComposition, SqliteJdbcComposition }
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.qzio.{ ZioJdbcContext, ZioJdbcUnderlyingContext }
import io.getquill.util.LoadConfig

import javax.sql.DataSource

class PostgresZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[PostgresDialect, N]
  with PostgresJdbcComposition[N] {

  val underlying: ZioJdbcUnderlyingContext[PostgresDialect, N] = new PostgresZioJdbcContext.Underlying[N](naming)
}
object PostgresZioJdbcContext {
  class Underlying[N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[PostgresDialect, N]
    with PostgresJdbcComposition[N]
    with JdbcRunContext[PostgresDialect, N]
}

class SqlServerZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[SQLServerDialect, N]
  with SqlServerJdbcComposition[N] {

  val underlying: ZioJdbcUnderlyingContext[SQLServerDialect, N] = new SqlServerZioJdbcContext.Underlying[N](naming)
}

object SqlServerZioJdbcContext {
  class Underlying[N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[SQLServerDialect, N]
    with SqlServerJdbcComposition[N]
    with JdbcRunContext[SQLServerDialect, N]
    with SqlServerExecuteOverride[N]
}

class H2ZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[H2Dialect, N]
  with H2JdbcComposition[N] {

  val underlying: ZioJdbcUnderlyingContext[H2Dialect, N] = new H2ZioJdbcContext.Underlying[N](naming)
}
object H2ZioJdbcContext {
  class Underlying[N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[H2Dialect, N]
    with H2JdbcComposition[N]
    with JdbcRunContext[H2Dialect, N]
}

class MysqlZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[MySQLDialect, N]
  with MysqlJdbcComposition[N] {

  val underlying: ZioJdbcUnderlyingContext[MySQLDialect, N] = new MysqlZioJdbcContext.Underlying[N](naming)
}
object MysqlZioJdbcContext {
  class Underlying[N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[MySQLDialect, N]
    with MysqlJdbcComposition[N]
    with JdbcRunContext[MySQLDialect, N]
}

class SqliteZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[SqliteDialect, N]
  with SqliteJdbcComposition[N] {

  val underlying: ZioJdbcUnderlyingContext[SqliteDialect, N] = new SqliteZioJdbcContext.Underlying[N](naming)
}
object SqliteZioJdbcContext {
  class Underlying[N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[SqliteDialect, N]
    with SqliteJdbcComposition[N]
    with JdbcRunContext[SqliteDialect, N]
}

class OracleZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[OracleDialect, N]
  with OracleJdbcComposition[N] {

  val underlying: ZioJdbcUnderlyingContext[OracleDialect, N] = new OracleZioJdbcContext.Underlying[N](naming)
}
object OracleZioJdbcContext {
  class Underlying[N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[OracleDialect, N]
    with OracleJdbcComposition[N]
    with JdbcRunContext[OracleDialect, N]
}

trait WithProbing[D <: SqlIdiom, N <: NamingStrategy] extends ZioJdbcUnderlyingContext[D, N] {
  def probingConfig: Config;
  override def probingDataSource: Option[DataSource] = Some(JdbcContextConfig(probingConfig).dataSource)
}

trait WithProbingPrefix[D <: SqlIdiom, N <: NamingStrategy] extends WithProbing[D, N] {
  def configPrefix: String;
  def probingConfig: Config = LoadConfig(configPrefix)
}
