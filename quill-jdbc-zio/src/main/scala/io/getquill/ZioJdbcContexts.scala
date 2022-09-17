package io.getquill

import com.typesafe.config.Config
import io.getquill.context.jdbc.{ H2JdbcTypes, MysqlJdbcTypes, OracleJdbcTypes, PostgresJdbcTypes, SqlServerExecuteOverride, SqlServerJdbcTypes, SqliteJdbcTypes }
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.qzio.{ ZioJdbcContext, ZioJdbcUnderlyingContext }
import io.getquill.util.LoadConfig

import javax.sql.DataSource

class PostgresZioJdbcContext[+N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[PostgresDialect, N]
  with PostgresJdbcTypes[PostgresDialect, N] {
  val idiom: PostgresDialect = PostgresDialect

  val connDelegate: ZioJdbcUnderlyingContext[PostgresDialect, N] = new PostgresZioJdbcContext.Underlying[N](naming)
}
object PostgresZioJdbcContext {
  class Underlying[+N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[PostgresDialect, N]
    with PostgresJdbcTypes[PostgresDialect, N] {
    val idiom: PostgresDialect = PostgresDialect
  }
}

class SqlServerZioJdbcContext[+N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[SQLServerDialect, N]
  with SqlServerJdbcTypes[SQLServerDialect, N] {
  val idiom: SQLServerDialect = SQLServerDialect

  val connDelegate: ZioJdbcUnderlyingContext[SQLServerDialect, N] = new SqlServerZioJdbcContext.Underlying[N](naming)
}

object SqlServerZioJdbcContext {
  class Underlying[+N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[SQLServerDialect, N]
    with SqlServerJdbcTypes[SQLServerDialect, N]
    with SqlServerExecuteOverride[N] {
    val idiom: SQLServerDialect = SQLServerDialect
  }
}

class H2ZioJdbcContext[+N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[H2Dialect, N]
  with H2JdbcTypes[H2Dialect, N] {
  val idiom: H2Dialect = H2Dialect

  val connDelegate: ZioJdbcUnderlyingContext[H2Dialect, N] = new H2ZioJdbcContext.Underlying[N](naming)
}
object H2ZioJdbcContext {
  class Underlying[+N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[H2Dialect, N]
    with H2JdbcTypes[H2Dialect, N] {
    val idiom: H2Dialect = H2Dialect
  }
}

class MysqlZioJdbcContext[+N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[MySQLDialect, N]
  with MysqlJdbcTypes[MySQLDialect, N] {
  val idiom: MySQLDialect = MySQLDialect

  val connDelegate: ZioJdbcUnderlyingContext[MySQLDialect, N] = new MysqlZioJdbcContext.Underlying[N](naming)
}
object MysqlZioJdbcContext {
  class Underlying[+N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[MySQLDialect, N]
    with MysqlJdbcTypes[MySQLDialect, N] {
    val idiom: MySQLDialect = MySQLDialect
  }
}

class SqliteZioJdbcContext[+N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[SqliteDialect, N]
  with SqliteJdbcTypes[SqliteDialect, N] {
  val idiom: SqliteDialect = SqliteDialect

  val connDelegate: ZioJdbcUnderlyingContext[SqliteDialect, N] = new SqliteZioJdbcContext.Underlying[N](naming)
}
object SqliteZioJdbcContext {
  class Underlying[+N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[SqliteDialect, N]
    with SqliteJdbcTypes[SqliteDialect, N] {
    val idiom: SqliteDialect = SqliteDialect
  }
}

class OracleZioJdbcContext[+N <: NamingStrategy](val naming: N)
  extends ZioJdbcContext[OracleDialect, N]
  with OracleJdbcTypes[OracleDialect, N] {
  val idiom: OracleDialect = OracleDialect

  val connDelegate: ZioJdbcUnderlyingContext[OracleDialect, N] = new OracleZioJdbcContext.Underlying[N](naming)
}
object OracleZioJdbcContext {
  class Underlying[+N <: NamingStrategy](val naming: N)
    extends ZioJdbcUnderlyingContext[OracleDialect, N]
    with OracleJdbcTypes[OracleDialect, N] {
    val idiom: OracleDialect = OracleDialect
  }
}

trait WithProbing[D <: SqlIdiom, +N <: NamingStrategy] extends ZioJdbcUnderlyingContext[D, N] {
  def probingConfig: Config;
  override def probingDataSource: Option[DataSource] = Some(JdbcContextConfig(probingConfig).dataSource)
}

trait WithProbingPrefix[D <: SqlIdiom, +N <: NamingStrategy] extends WithProbing[D, N] {
  def configPrefix: String;
  def probingConfig: Config = LoadConfig(configPrefix)
}
