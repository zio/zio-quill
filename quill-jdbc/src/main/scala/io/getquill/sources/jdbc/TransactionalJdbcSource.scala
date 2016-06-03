package io.getquill.sources.jdbc

import java.sql.Connection

import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.idiom.SqlIdiom

class TransactionalJdbcSource[D <: SqlIdiom, N <: NamingStrategy](conn: Connection) extends JdbcSource[D, N] {

  conn.setAutoCommit(false)

  protected def connection = conn

  def commit() = connection.commit

  def rollback() = connection.rollback

  def release() = connection.setAutoCommit(true)

  override def close = connection.close
}
