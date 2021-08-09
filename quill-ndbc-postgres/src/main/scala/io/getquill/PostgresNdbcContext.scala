package io.getquill

import com.typesafe.config.Config
import io.getquill.context.ndbc._
import io.getquill.util.LoadConfig
import io.trane.ndbc._

class PostgresNdbcContext[N <: NamingStrategy](naming: N, dataSource: DataSource[PostgresPreparedStatement, PostgresRow])
  extends NdbcContext[PostgresDialect, N, PostgresPreparedStatement, PostgresRow](PostgresDialect, naming, dataSource)
  with PostgresNdbcContextBase[N] {

  def this(naming: N, config: NdbcContextConfig) = this(naming, PostgresDataSource.create(config.dataSource))
  def this(naming: N, config: Config) = this(naming, NdbcContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))
}