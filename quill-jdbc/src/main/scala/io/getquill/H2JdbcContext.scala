package io.getquill

import java.io.Closeable

import javax.sql.DataSource
import com.typesafe.config.Config
import io.getquill.context.jdbc.{ JdbcContext, H2JdbcContextBase }
import io.getquill.util.LoadConfig

class H2JdbcContext[N <: NamingStrategy](val naming: N, val dataSource: DataSource with Closeable)
  extends JdbcContext[H2Dialect, N]
  with H2JdbcContextBase[N] {

  def this(naming: N, config: JdbcContextConfig) = this(naming, config.dataSource)
  def this(naming: N, config: Config) = this(naming, JdbcContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))
}
