package io.getquill

import com.typesafe.config.Config
import io.getquill.context.jdbc.{ H2JdbcTypes, MysqlJdbcTypes, OracleJdbcTypes, PostgresJdbcTypes, SqlServerExecuteOverride, SqlServerJdbcTypes, SqliteJdbcTypes }
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.qzio.{ ZioJdbcContext, ZioJdbcUnderlyingContext }
import io.getquill.util.LoadConfig

import javax.sql.DataSource

class PostgresZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[PostgresDialect, N]
  with PostgresJdbcTypes[N] {

  val underlying: ZioJdbcUnderlyingContext[PostgresDialect, N] = new PostgresZioJdbcContext.Underlying[N](naming)
}
object PostgresZioJdbcContext {
  class Underlying[N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[PostgresDialect, N]
    with PostgresJdbcTypes[N]
}

class SqlServerZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[SQLServerDialect, N]
  with SqlServerJdbcTypes[N] {

  val underlying: ZioJdbcUnderlyingContext[SQLServerDialect, N] = new SqlServerZioJdbcContext.Underlying[N](naming)
}

object SqlServerZioJdbcContext {
  class Underlying[N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[SQLServerDialect, N]
    with SqlServerJdbcTypes[N]
    with SqlServerExecuteOverride[N]
}

class H2ZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[H2Dialect, N]
  with H2JdbcTypes[N] {

  val underlying: ZioJdbcUnderlyingContext[H2Dialect, N] = new H2ZioJdbcContext.Underlying[N](naming)
}
object H2ZioJdbcContext {
  class Underlying[N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[H2Dialect, N]
    with H2JdbcTypes[N]
}

class MysqlZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[MySQLDialect, N]
  with MysqlJdbcTypes[N] {

  val underlying: ZioJdbcUnderlyingContext[MySQLDialect, N] = new MysqlZioJdbcContext.Underlying[N](naming)
}
object MysqlZioJdbcContext {
  class Underlying[N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[MySQLDialect, N]
    with MysqlJdbcTypes[N]
}

class SqliteZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[SqliteDialect, N]
  with SqliteJdbcTypes[N] {

  val underlying: ZioJdbcUnderlyingContext[SqliteDialect, N] = new SqliteZioJdbcContext.Underlying[N](naming)
}
object SqliteZioJdbcContext {
  class Underlying[N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[SqliteDialect, N]
    with SqliteJdbcTypes[N]
}

class OracleZioJdbcContext[N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[OracleDialect, N]
  with OracleJdbcTypes[N] {

  val underlying: ZioJdbcUnderlyingContext[OracleDialect, N] = new OracleZioJdbcContext.Underlying[N](naming)
}
object OracleZioJdbcContext {
  class Underlying[N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[OracleDialect, N]
    with OracleJdbcTypes[N]
}

trait WithProbing[D <: SqlIdiom, N <: NamingStrategy] extends ZioJdbcUnderlyingContext[D, N] {
  def probingConfig: Config;
  override def probingDataSource: Option[DataSource] = Some(JdbcContextConfig(probingConfig).dataSource)
}

trait WithProbingPrefix[D <: SqlIdiom, N <: NamingStrategy] extends WithProbing[D, N] {
  def configPrefix: String;
  def probingConfig: Config = LoadConfig(configPrefix)
}
