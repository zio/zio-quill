package io.getquill

import com.typesafe.config.Config

import io.getquill.util.LoadConfig
import io.trane.ndbc.DataSource
import io.getquill.context.ndbc.StandardDecoders
import io.getquill.context.ndbc.StandardEncoders
import io.trane.ndbc.PreparedStatement
import io.trane.ndbc.Row

class MysqlNdbcContext[N <: NamingStrategy](naming: N, dataSource: DataSource[PreparedStatement, Row])
  extends NdbcContext[MySQLDialect, N](MySQLDialect, naming, dataSource)
  with StandardEncoders
  with StandardDecoders {

  def this(naming: N, config: NdbcContextConfig) = this(naming, config.dataSource)
  def this(naming: N, config: Config) = this(naming, NdbcContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))
}
