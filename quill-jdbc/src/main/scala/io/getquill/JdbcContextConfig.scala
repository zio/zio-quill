package io.getquill

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.Properties

case class JdbcContextConfig(config: Config) {

  def configProperties = {
    import scala.collection.JavaConverters._
    val p = new Properties
    for (entry <- config.entrySet.asScala)
      p.setProperty(entry.getKey, entry.getValue.unwrapped.toString)
    p
  }

  def dataSource =
    new HikariDataSource(new HikariConfig(configProperties))
}
