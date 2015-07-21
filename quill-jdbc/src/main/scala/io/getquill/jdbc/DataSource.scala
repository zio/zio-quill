package io.getquill.jdbc

import scala.collection.JavaConversions._
import com.typesafe.config.Config
import java.util.Properties
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object DataSource {

  def apply(config: Config): javax.sql.DataSource =
    new HikariDataSource(new HikariConfig(toProperties(config)))

  private def toProperties(config: Config) = {
    val p = new Properties
    for (entry <- config.entrySet)
      p.setProperty(entry.getKey, entry.getValue.render)
    p
  }
}