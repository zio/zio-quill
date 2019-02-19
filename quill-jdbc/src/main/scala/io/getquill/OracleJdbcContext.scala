package io.getquill

import java.io.Closeable

import com.typesafe.config.Config
import io.getquill.context.jdbc.{ JdbcContext, OracleJdbcContextBase }
import io.getquill.util.LoadConfig
import javax.sql.DataSource

class OracleJdbcContext[N <: NamingStrategy](val naming: N, val dataSource: DataSource with Closeable)
  extends JdbcContext[OracleDialect, N]
  with OracleJdbcContextBase[N] {

  def this(naming: N, config: JdbcContextConfig) = this(naming, config.dataSource)
  def this(naming: N, config: Config) = this(naming, JdbcContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))
}
