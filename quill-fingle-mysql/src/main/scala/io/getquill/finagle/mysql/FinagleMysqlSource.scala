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
import com.twitter.util.Local
import com.twitter.util.Return
import com.twitter.finagle.exp.mysql.Result

trait FinagleMysqlSource extends SqlSource[Row, List[Parameter]] with StrictLogging {

  protected val client = FinagleMysqlClient(config)

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

  private val currentClient = new Local[Client]

  def transaction[T](f: => Future[T]) =
    client.transaction {
      transactional =>
        currentClient.update(transactional)
        f.interruptible.ensure(currentClient.clear)
    }

  def execute(sql: String, bindList: List[List[Parameter] => List[Parameter]]): Future[List[Result]] =
    bindList match {
      case bind :: Nil =>
        withClient(_.prepare(sql)(bind(List()): _*)).map(List(_))
      case bind :: tail =>
        withClient(_.prepare(sql)(bind(List()): _*))
          .flatMap(_ => execute(sql, tail))
      case Nil =>
        withClient(_.prepare(sql)(List(): _*)).map(List(_))
    }

  def query[T](sql: String, bind: List[Parameter] => List[Parameter], extractor: Row => T) = {
    logger.debug(sql)
    withClient(_.prepare(sql).select(bind(List()): _*)(extractor))
  }

  private def withClient[T](f: Client => T) =
    currentClient().map(f).getOrElse {
      f(client)
    }
}
