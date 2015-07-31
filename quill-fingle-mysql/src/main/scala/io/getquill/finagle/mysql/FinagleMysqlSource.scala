package io.getquill.finagle.mysql

import io.getquill.sql.SqlSource
import com.twitter.finagle.exp.mysql.Row
import com.twitter.finagle.exp.mysql.Value
import com.twitter.finagle.exp.Mysql
import com.typesafe.scalalogging.StrictLogging
import com.twitter.finagle.exp.mysql.Parameter
import com.twitter.finagle.exp.mysql.LongValue
import com.twitter.finagle.exp.mysql.IntValue
import com.twitter.finagle.exp.mysql.StringValue
import com.twitter.finagle.exp.mysql.CanBeParameter._
import com.twitter.finagle.exp.mysql.Parameter._

trait FinagleMysqlSource extends SqlSource[Row, List[Parameter]] with StrictLogging {

  private val mysql = FinagleMysqlClient(config)

  implicit val longDecoder = new Decoder[Long] {
    def apply(index: Int, row: Row) =
      row.values(index) match {
        case LongValue(long) => long
        case other           => throw new IllegalStateException(s"Invalid column value $other")
      }
  }

  implicit val longEncoder = new Encoder[Long] {
    def apply(index: Int, value: Long, row: List[Parameter]) = {
      row :+ (value: Parameter)
    }
  }

  implicit val intDecoder = new Decoder[Int] {
    def apply(index: Int, row: Row) =
      row.values(index) match {
        case IntValue(int)   => int
        case LongValue(long) => long.toInt
        case other           => throw new IllegalStateException(s"Invalid column value $other")
      }
  }

  implicit val intEncoder = new Encoder[Int] {
    def apply(index: Int, value: Int, row: List[Parameter]) = {
      row :+ (value: Parameter)
    }
  }

  implicit val stringDecoder = new Decoder[String] {
    def apply(index: Int, row: Row) =
      row.values(index) match {
        case StringValue(long) => long
        case other             => throw new IllegalStateException(s"Invalid column value $other")
      }
  }

  implicit val stringEncoder = new Encoder[String] {
    def apply(index: Int, value: String, row: List[Parameter]) = {
      row :+ (value: Parameter)
    }
  }

  def run[T](sql: String, bind: List[Parameter] => List[Parameter], extractor: Row => T) = {
    logger.debug(sql)
    mysql.prepare(sql).select(bind(List()): _*)(extractor)
  }

}