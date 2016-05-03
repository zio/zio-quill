package io.getquill.sources.jdbc

import java.sql.Connection

import io.getquill.JdbcSourceConfig
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.idiom.SqlIdiom

import scala.util.control.NonFatal

class TransactionalJdbcSource[D <: SqlIdiom, N <: NamingStrategy, T](config: JdbcSourceConfig[D, N], conn: Connection)
  extends JdbcSource[D, N](config) {

  conn.setAutoCommit(false)

  protected def connection = conn

  def execute(f: JdbcSource[D, N] => T) = {
    try {
      val res = f(this)
      connection.commit
      res
    } catch {
      case NonFatal(e) =>
        connection.rollback
        throw e
    } finally {
      connection.setAutoCommit(true)
    }
  }
}
