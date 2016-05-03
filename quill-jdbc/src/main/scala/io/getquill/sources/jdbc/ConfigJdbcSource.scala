package io.getquill.sources.jdbc

import java.sql.Connection

import io.getquill.JdbcSourceConfig
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.idiom.SqlIdiom

import scala.util.DynamicVariable

class ConfigJdbcSource[D <: SqlIdiom, N <: NamingStrategy](config: JdbcSourceConfig[D, N])
  extends JdbcSource[D, N](config) {

  private val currentConnection = new DynamicVariable[Option[Connection]](Some(dataSource.getConnection))

  protected def connection = currentConnection.value.getOrElse(dataSource.getConnection)
}
