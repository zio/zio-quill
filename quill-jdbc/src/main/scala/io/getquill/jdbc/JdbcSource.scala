package io.getquill.jdbc

import java.sql.Connection
import java.sql.ResultSet
import io.getquill.sql.SqlSource
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariConfig

trait JdbcSource extends SqlSource[ResultSet] {

  implicit val longEncoder = new Encoder[Long] {
    def encode(value: Long, index: Int, row: ResultSet) = ???
    def decode(index: Int, row: ResultSet) =
      row.getLong(index)
  }

  implicit val intEncoder = new Encoder[Int] {
    def encode(value: Int, index: Int, row: ResultSet) = ???
    def decode(index: Int, row: ResultSet) =
      row.getInt(index)
  }

  implicit val stringEncoder = new Encoder[String] {
    def encode(value: String, index: Int, row: ResultSet) = ???
    def decode(index: Int, row: ResultSet) =
      row.getString(index)
  }

  def run[T](sql: String, extractor: ResultSet => T) = {
    val conn = dataSource.getConnection
    try extractor(conn.prepareStatement(sql).executeQuery)
    finally conn.close
  }

  private val dataSource: HikariDataSource =
    new HikariDataSource(DataSourceConfig(config))
}
