package io.getquill.jdbc

import scala.collection.JavaConversions._
import com.typesafe.config.Config
import java.util.Properties
import com.zaxxer.hikari.HikariConfig

object DataSourceConfig {

  def apply(config: Config) =
    new HikariConfig(toProperties(config))

  private def toProperties(config: Config) = {
    val p = new Properties
    for (entry <- config.entrySet)
      p.setProperty(entry.getKey, entry.getValue.render)
    p
  }
}