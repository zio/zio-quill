package io.getquill

import com.typesafe.config.Config

import io.getquill.context.ndbc.BaseNdbcContext
import io.getquill.context.sql.encoding.ArrayEncoding
import io.getquill.util.LoadConfig
import io.trane.ndbc.DataSource
import io.trane.ndbc.PostgresPreparedStatement
import io.trane.ndbc.PostgresRow
import io.trane.ndbc.PostgresDataSource
import io.getquill.context.ndbc.PostgresEncoders
import io.getquill.context.ndbc.PostgresDecoders

class PostgresNdbcContext[N <: NamingStrategy](naming: N, dataSource: DataSource[PostgresPreparedStatement, PostgresRow])
  extends BaseNdbcContext[PostgresDialect, N, PostgresPreparedStatement, PostgresRow](PostgresDialect, naming, dataSource)
  with ArrayEncoding
  with PostgresEncoders
  with PostgresDecoders {

  def this(naming: N, config: NdbcContextConfig) = this(naming, PostgresDataSource.apply(config.dataSource))
  def this(naming: N, config: Config) = this(naming, NdbcContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  protected def createPreparedStatement(sql: String) = PostgresPreparedStatement.apply(sql)
}
