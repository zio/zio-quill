package io.getquill

import com.typesafe.config.Config

import io.getquill.context.ndbc._
import io.getquill.context.sql.encoding.ArrayEncoding
import io.getquill.util.LoadConfig
import io.trane.ndbc._

class NdbcPostgresContext[N <: NamingStrategy](naming: N, dataSource: DataSource[PostgresPreparedStatement, PostgresRow])
  extends NdbcContext[PostgresDialect, N, PostgresPreparedStatement, PostgresRow](PostgresDialect, naming, dataSource)
  with ArrayEncoding
  with PostgresEncoders
  with PostgresDecoders {

  def this(naming: N, config: NdbcContextConfig) = this(naming, PostgresDataSource.create(config.dataSource))
  def this(naming: N, config: Config) = this(naming, NdbcContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  protected def createPreparedStatement(sql: String) = PostgresPreparedStatement.create(sql)

  override protected val effect = NdbcContextEffect
}