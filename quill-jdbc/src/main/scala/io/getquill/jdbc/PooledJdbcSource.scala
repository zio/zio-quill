package io.getquill.jdbc

import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import com.typesafe.scalalogging.StrictLogging
import io.getquill.sql.SqlSource
import java.sql.PreparedStatement
import java.sql.Connection

trait PooledJdbcSource extends JdbcSource with StrictLogging {

  private val dataSource = DataSource(config)

  override protected def withConnection[T](f: Connection => T) = {
    val conn = dataSource.getConnection
    conn.setAutoCommit(true)
    try f(conn)
    finally conn.close
  }

}
