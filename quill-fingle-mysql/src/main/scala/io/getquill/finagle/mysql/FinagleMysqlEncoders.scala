package io.getquill.finagle.mysql

import scala.reflect.ClassTag
import com.twitter.finagle.exp.mysql.CanBeParameter
import com.twitter.finagle.exp.mysql.CanBeParameter._
import com.twitter.finagle.exp.mysql.Parameter
import java.util.Date
import com.twitter.finagle.exp.mysql.transport.BufferWriter
import com.twitter.finagle.exp.mysql.BigDecimalValue
import com.twitter.finagle.exp.mysql.Type
import language.implicitConversions

trait FinagleMysqlEncoders {
  this: FinagleMysqlSource =>

  def encoder[T](implicit cbp: CanBeParameter[T]): Encoder[T] =
    new Encoder[T] {
      def apply(index: Int, value: T, row: List[Parameter]) =
        row :+ (value: Parameter)
    }

  implicit val stringEncoder = encoder[String]
  implicit val bigDecimalEncoder =
    new Encoder[BigDecimal] {
      def apply(index: Int, value: BigDecimal, row: List[Parameter]) =
        row :+ (BigDecimalValue(value): Parameter)
    }
  implicit val booleanEncoder = encoder[Boolean]
  implicit val byteEncoder = encoder[Byte]
  implicit val shortEncoder = encoder[Short]
  implicit val intEncoder = encoder[Int]
  implicit val longEncoder = encoder[Long]
  implicit val floatEncoder = encoder[Float]
  implicit val doubleEncoder = encoder[Double]
  implicit val byteArrayEncoder = encoder[Array[Byte]]
  implicit val dateEncoder = encoder[Date]
}
