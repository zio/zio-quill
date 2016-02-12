package io.getquill.source.jdbc

import java.util.Properties

import scala.collection.JavaConversions.asScalaSet

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object DataSource {

  def apply(config: Config): javax.sql.DataSource with java.io.Closeable =
    new HikariDataSource(new HikariConfig(toProperties(config)))

  private def toProperties(config: Config) = {
    val p = new Properties
    for (entry <- config.entrySet if (entry.getKey != "queryProbing"))
      p.setProperty(entry.getKey, entry.getValue.unwrapped.toString)
    p
  }
}
