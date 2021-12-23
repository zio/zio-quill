package io.getquill

import com.typesafe.config.Config
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }

import java.io.Closeable
import java.util.Properties
import scala.util.control.NonFatal

final case class JdbcContextConfig(config: Config) {
  type DataSource = javax.sql.DataSource with Closeable

  def configProperties: Properties = {
    import scala.jdk.CollectionConverters._
    val p = new Properties
    for (entry <- config.entrySet.asScala)
      p.setProperty(entry.getKey, entry.getValue.unwrapped.toString)
    p
  }

  def dataSource: DataSource =
    try
      new HikariDataSource(new HikariConfig(configProperties))
    catch {
      case NonFatal(ex) =>
        throw new IllegalStateException(s"Failed to load data source for config: '$config'", ex)
    }
}
