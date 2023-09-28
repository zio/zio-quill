package io.getquill

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

import java.util.Properties
import scala.util.control.NonFatal

final case class JdbcContextConfig(config: Config) {

  def configProperties: Properties = {
    import scala.jdk.CollectionConverters._
    val p = new Properties
    for (entry <- config.entrySet.asScala)
      p.setProperty(entry.getKey, entry.getValue.unwrapped.toString)
    p
  }

  def dataSource: HikariDataSource =
    try
      new HikariDataSource(new HikariConfig(configProperties))
    catch {
      case NonFatal(ex) =>
        throw new IllegalStateException("Failed to load data source", ex)
    }
}
