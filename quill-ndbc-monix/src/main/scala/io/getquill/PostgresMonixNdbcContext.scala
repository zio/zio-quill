package io.getquill

import com.typesafe.config.Config
import io.getquill.context.monix.MonixNdbcContext
import io.getquill.context.monix.MonixNdbcContext.Runner
import io.getquill.context.ndbc.{ NdbcContextConfig, PostgresNdbcContextBase }
import io.getquill.util.LoadConfig
import io.trane.ndbc.{ DataSource, PostgresDataSource, PostgresPreparedStatement, PostgresRow }

class PostgresMonixNdbcContext[N <: NamingStrategy](
  val naming:     N,
  val dataSource: DataSource[PostgresPreparedStatement, PostgresRow],
  runner:         Runner
) extends MonixNdbcContext[PostgresDialect, N, PostgresPreparedStatement, PostgresRow](dataSource, runner)
  with PostgresNdbcContextBase[N] {

  def this(naming: N, config: NdbcContextConfig, runner: Runner) = this(naming, PostgresDataSource.create(config.dataSource), runner)
  def this(naming: N, config: Config, runner: Runner) = this(naming, NdbcContextConfig(config), runner)
  def this(naming: N, configPrefix: String, runner: Runner) = this(naming, LoadConfig(configPrefix), runner)
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix), Runner.default)
}