package io.getquill.jdbc

import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import com.typesafe.scalalogging.StrictLogging
import io.getquill.sql.SqlSource
import java.sql.PreparedStatement

trait JdbcSource extends SqlSource[ResultSet, PreparedStatement] with StrictLogging {

  implicit val longDecoder = new Decoder[Long] {
    def apply(index: Int, row: ResultSet) =
      row.getLong(index + 1)
  }

  implicit val longEncoder = new Encoder[Long] {
    def apply(index: Int, row: PreparedStatement, value: Long) =
      row.setLong(index + 1, value)
  }

  implicit val intDecoder = new Decoder[Int] {
    def apply(index: Int, row: ResultSet) =
      row.getInt(index + 1)
  }

  implicit val intEncoder = new Encoder[Int] {
    def apply(index: Int, row: PreparedStatement, value: Int) =
      row.setInt(index + 1, value)
  }

  implicit val stringDecoder = new Decoder[String] {
    def apply(index: Int, row: ResultSet) =
      row.getString(index + 1)
  }

  implicit val stringEncoder = new Encoder[String] {
    def apply(index: Int, row: PreparedStatement, value: String) =
      row.setString(index + 1, value)
  }

  private val dataSource = DataSource(config)

  def run[T](sql: String, extractor: ResultSet => T): List[T] = {
    run(sql, null, extractor)
  }

  def run[T](sql: String, bind: PreparedStatement => Unit, extractor: ResultSet => T) = {
    logger.debug(sql)
    val conn = dataSource.getConnection
    try {
      val ps = conn.prepareStatement(sql)
      bind(ps)
      val rs = ps.executeQuery
      val result = new ListBuffer[T]
      while (rs.next)
        result += extractor(rs)
      result.toList
    }
    finally conn.close
  }
}
