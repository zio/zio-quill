package io.getquill

import java.io.Closeable
import javax.sql.DataSource

import com.typesafe.config.Config
import io.getquill.context.jdbc.{ JdbcContext, UUIDObjectEncoding }
import io.getquill.util.LoadConfig

class PostgresJdbcContext[N <: NamingStrategy](dataSource: DataSource with Closeable)
  extends JdbcContext[PostgresDialect, N](dataSource) with UUIDObjectEncoding {

  def this(config: JdbcContextConfig) = this(config.dataSource)
  def this(config: Config) = this(JdbcContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

}
