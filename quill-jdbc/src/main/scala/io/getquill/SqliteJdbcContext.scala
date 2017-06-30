package io.getquill

import java.io.Closeable
import javax.sql.DataSource

import com.typesafe.config.Config
import io.getquill.context.jdbc.{ JdbcContext, UUIDStringEncoding }
import io.getquill.util.LoadConfig

class SqliteJdbcContext[N <: NamingStrategy](dataSource: DataSource with Closeable)
  extends JdbcContext[SqliteDialect, N](dataSource) with UUIDStringEncoding {

  def this(config: JdbcContextConfig) = this(config.dataSource)
  def this(config: Config) = this(JdbcContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

}
