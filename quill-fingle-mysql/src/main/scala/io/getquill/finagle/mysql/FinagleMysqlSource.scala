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
import com.twitter.finagle.exp.mysql.Client
import com.twitter.util.Future
import com.twitter.finagle.exp.mysql.CanBeParameter
import scala.reflect.ClassTag

trait FinagleMysqlSource extends SqlSource[Row, List[Parameter]] with StrictLogging {

  protected val client: Client

  class ParameterEncoder[T: ClassTag](implicit cbp: CanBeParameter[T]) extends Encoder[T] {
    def apply(index: Int, value: T, row: List[Parameter]) =
      row :+ (value: Parameter)
  }

  implicit val longDecoder = new Decoder[Long] {
    def apply(index: Int, row: Row) =
      row.values(index) match {
        case LongValue(long) => long
        case other           => throw new IllegalStateException(s"Invalid column value $other")
      }
  }

  implicit val longEncoder = new ParameterEncoder[Long]

  implicit val intDecoder = new Decoder[Int] {
    def apply(index: Int, row: Row) =
      row.values(index) match {
        case IntValue(int)   => int
        case LongValue(long) => long.toInt
        case other           => throw new IllegalStateException(s"Invalid column value $other")
      }
  }

  implicit val intEncoder = new ParameterEncoder[Int]

  implicit val stringDecoder = new Decoder[String] {
    def apply(index: Int, row: Row) =
      row.values(index) match {
        case StringValue(long) => long
        case other             => throw new IllegalStateException(s"Invalid column value $other")
      }
  }

  implicit val stringEncoder = new ParameterEncoder[String]

  def run[T](sql: String, bind: List[Parameter] => List[Parameter], extractor: Row => T) = {
    logger.debug(sql)
    client.prepare(sql).select(bind(List()): _*)(extractor)
  }

}