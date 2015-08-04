package io.getquill.jdbc

import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import com.typesafe.scalalogging.StrictLogging
import io.getquill.sql.SqlSource
import java.sql.PreparedStatement
import java.sql.Connection
import scala.util.control.NonFatal
import scala.util.DynamicVariable

trait JdbcSource extends SqlSource[ResultSet, PreparedStatement] with StrictLogging {

  implicit val longDecoder = new Decoder[Long] {
    def apply(index: Int, row: ResultSet) =
      row.getLong(index + 1)
  }

  implicit val longEncoder = new Encoder[Long] {
    def apply(index: Int, value: Long, row: PreparedStatement) = {
      row.setLong(index + 1, value)
      row
    }
  }

  implicit val intDecoder = new Decoder[Int] {
    def apply(index: Int, row: ResultSet) =
      row.getInt(index + 1)
  }

  implicit val intEncoder = new Encoder[Int] {
    def apply(index: Int, value: Int, row: PreparedStatement) = {
      row.setInt(index + 1, value)
      row
    }
  }

  implicit val stringDecoder = new Decoder[String] {
    def apply(index: Int, row: ResultSet) =
      row.getString(index + 1)
  }

  implicit val stringEncoder = new Encoder[String] {
    def apply(index: Int, value: String, row: PreparedStatement) = {
      row.setString(index + 1, value)
      row
    }
  }

  protected val dataSource = DataSource(config)

  private val currentConnection = new DynamicVariable[Option[Connection]](None)

  protected def withConnection[T](f: Connection => T) =
    currentConnection.value.map(f).getOrElse {
      val conn = dataSource.getConnection
      conn.setAutoCommit(true)
      try f(conn)
      finally conn.close
    }

  def transaction[T](f: => T) =
    withConnection { conn =>
      currentConnection.withValue(Some(conn)) {
        conn.setAutoCommit(false)
        try {
          val res = f
          conn.commit
          res
        } catch {
          case NonFatal(e) =>
            conn.rollback
            throw e
        }
      }
    }

  def insertRun(sql: String, bindList: List[PreparedStatement => PreparedStatement]) = {
    logger.debug(sql)
    withConnection { conn =>
      val ps = conn.prepareStatement(sql)
      for (bind <- bindList) {
        bind(ps)
        ps.addBatch
      }
      ps.executeBatch
    }
  }

  def queryRun[T](sql: String, bind: PreparedStatement => PreparedStatement, extractor: ResultSet => T) = {
    logger.debug(sql)
    withConnection { conn =>
      val ps = bind(conn.prepareStatement(sql))
      val rs = ps.executeQuery
      val result = new ListBuffer[T]
      while (rs.next)
        result += extractor(rs)
      result.toList
    }
  }
}
