package io.getquill.jdbc

import java.sql.Connection
import java.sql.ResultSet
import io.getquill.sql.SqlSource
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariConfig
import scala.collection.mutable.ListBuffer
import com.typesafe.scalalogging.StrictLogging

trait JdbcSource extends SqlSource[ResultSet] with StrictLogging {

  implicit val longEncoder = new Encoder[Long] {
    def encode(value: Long, index: Int, row: ResultSet) = ???
    def decode(index: Int, row: ResultSet) =
      row.getLong(index + 1)
  }

  implicit val intEncoder = new Encoder[Int] {
    def encode(value: Int, index: Int, row: ResultSet) = ???
    def decode(index: Int, row: ResultSet) =
      row.getInt(index + 1)
  }

  implicit val stringEncoder = new Encoder[String] {
    def encode(value: String, index: Int, row: ResultSet) = ???
    def decode(index: Int, row: ResultSet) =
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
    } finally conn.close
  }
}
