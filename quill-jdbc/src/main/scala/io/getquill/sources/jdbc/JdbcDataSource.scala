package io.getquill.sources.jdbc

import java.io.Closeable
import java.util.Properties

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

import javax.sql.DataSource

object CreateDataSource {

  def apply(config: Config): DataSource with Closeable = {
    def configProperties = {
      import scala.collection.JavaConverters._
      val p = new Properties
      for (entry <- config.entrySet.asScala)
        p.setProperty(entry.getKey, entry.getValue.unwrapped.toString)
      p
    }
    new HikariDataSource(new HikariConfig(configProperties))
  }
}
