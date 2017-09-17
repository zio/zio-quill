package io.getquill

import java.io.Closeable
import java.sql.Types
import javax.sql.DataSource

import com.typesafe.config.Config
import io.getquill.context.jdbc.{ ArrayDecoders, ArrayEncoders, JdbcContext, UUIDObjectEncoding }
import io.getquill.util.LoadConfig

class PostgresJdbcContext[N <: NamingStrategy](val naming: N, dataSource: DataSource with Closeable)
  extends JdbcContext[PostgresDialect, N](dataSource)
  with UUIDObjectEncoding
  with ArrayDecoders
  with ArrayEncoders {

  def this(naming: N, config: JdbcContextConfig) = this(naming, config.dataSource)
  def this(naming: N, config: Config) = this(naming, JdbcContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  val idiom = PostgresDialect

  override def parseJdbcType(intType: Int): String = intType match {
    case Types.TINYINT => super.parseJdbcType(Types.SMALLINT)
    case Types.VARCHAR => "text"
    case Types.DOUBLE  => "float8"
    case _             => super.parseJdbcType(intType)
  }
}
