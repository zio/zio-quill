package io.getquill

import java.util.Properties

import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import io.getquill.naming.NamingStrategy
import io.getquill.sources.SourceConfig
import io.getquill.sources.jdbc.ConnectionJdbcSource
import io.getquill.sources.sql.idiom.SqlIdiom

import scala.collection.JavaConversions._

class JdbcSourceConfig[D <: SqlIdiom, N <: NamingStrategy](val name: String) extends SourceConfig[ConnectionJdbcSource[D, N]] {

  def configProperties = {
    val p = new Properties
    for (entry <- config.entrySet)
      p.setProperty(entry.getKey, entry.getValue.unwrapped.toString)
    p
  }

  def dataSource: javax.sql.DataSource with java.io.Closeable =
    new HikariDataSource(new HikariConfig(configProperties))
}
