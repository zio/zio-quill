package io.getquill

import com.typesafe.config.Config

import io.getquill.context.ndbc.BaseNdbcContext
import io.getquill.context.ndbc.StandardDecoders
import io.getquill.context.ndbc.StandardEncoders
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.LoadConfig
import io.trane.ndbc.DataSource
import io.trane.ndbc.PreparedStatement
import io.trane.ndbc.Row

class NdbcContext[I <: SqlIdiom, N <: NamingStrategy](idiom: I, naming: N, dataSource: DataSource[PreparedStatement, Row])
  extends BaseNdbcContext[MySQLDialect, N, PreparedStatement, Row](MySQLDialect, naming, dataSource)
  with StandardEncoders
  with StandardDecoders {

  def this(idiom: I, naming: N, config: NdbcContextConfig) = this(idiom, naming, config.dataSource)
  def this(idiom: I, naming: N, config: Config) = this(idiom, naming, NdbcContextConfig(config))
  def this(idiom: I, naming: N, configPrefix: String) = this(idiom, naming, LoadConfig(configPrefix))

  protected def createPreparedStatement(sql: String) = PreparedStatement.create(sql)
}
