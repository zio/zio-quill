package io.getquill.jdbc

import java.sql.ResultSet

import scala.collection.mutable.ListBuffer

import com.typesafe.scalalogging.StrictLogging

import io.getquill.sql.SqlSource

trait JdbcSource extends SqlSource[ResultSet] with StrictLogging {

  implicit val longDecoder = new Decoder[Long] {
    def apply(index: Int, row: ResultSet) =
      row.getLong(index + 1)
  }

  implicit val intDecoder = new Decoder[Int] {
    def apply(index: Int, row: ResultSet) =
      row.getInt(index + 1)
  }

  implicit val stringDecoder = new Decoder[String] {
    def apply(index: Int, row: ResultSet) =
      row.getString(index + 1)
  }

  private val dataSource = DataSource(config)

  def run[T](sql: String, extractor: ResultSet => T) = {
    logger.debug(sql)
    val conn = dataSource.getConnection
    try {
      val rs = conn.prepareStatement(sql).executeQuery
      val result = new ListBuffer[T]
      while (rs.next)
        result += extractor(rs)
      result.toList
    }
    finally conn.close
  }
}
