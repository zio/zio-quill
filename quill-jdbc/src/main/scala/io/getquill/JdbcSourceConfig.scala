package io.getquill

import scala.collection.JavaConversions._
import java.util.Properties
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.getquill.sources.sql.idiom.SqlIdiom
import io.getquill.naming.NamingStrategy
import io.getquill.sources.SourceConfig
import io.getquill.sources.jdbc.JdbcSource

class JdbcSourceConfig[D <: SqlIdiom, N <: NamingStrategy](val name: String) extends SourceConfig[JdbcSource[D, N]] {

  def configProperties = {
    val p = new Properties
    for (entry <- config.entrySet if(entry.getKey != "queryProbing"))
      p.setProperty(entry.getKey, entry.getValue.unwrapped.toString)
    p
  }
  
  def dataSource: javax.sql.DataSource with java.io.Closeable =
      new HikariDataSource(new HikariConfig(configProperties))
}
