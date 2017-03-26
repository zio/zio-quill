package io.getquill

import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.jdbc.UUIDStringEncoding
import io.getquill.util.LoadConfig
import com.typesafe.config.Config
import java.io.Closeable
import javax.sql.DataSource

class SqlServerJdbcContext[N <: NamingStrategy](dataSource: DataSource with Closeable)
  extends JdbcContext[SQLServerDialect, N](dataSource) with UUIDStringEncoding {

  def this(config: JdbcContextConfig) = this(config.dataSource)
  def this(config: Config) = this(JdbcContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))
}