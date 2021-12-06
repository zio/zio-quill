package io.getquill

import java.io.Closeable

import com.typesafe.config.Config
import io.getquill.context.jdbc.SqliteJdbcContextBase
import io.getquill.context.monix.MonixJdbcContext.EffectWrapper
import io.getquill.context.monix.MonixJdbcContext
import io.getquill.util.LoadConfig
import javax.sql.DataSource

class SqliteMonixJdbcContext[N <: NamingStrategy](
  val naming:     N,
  val dataSource: DataSource with Closeable,
  runner:         EffectWrapper
) extends MonixJdbcContext[SqliteDialect, N](dataSource, runner)
  with SqliteJdbcContextBase[N] {

  def this(naming: N, config: JdbcContextConfig, runner: EffectWrapper) = this(naming, config.dataSource, runner)
  def this(naming: N, config: Config, runner: EffectWrapper) = this(naming, JdbcContextConfig(config), runner)
  def this(naming: N, configPrefix: String, runner: EffectWrapper) = this(naming, LoadConfig(configPrefix), runner)
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix), EffectWrapper.default)
}