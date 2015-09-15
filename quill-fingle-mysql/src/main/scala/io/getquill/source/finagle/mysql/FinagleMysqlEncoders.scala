package io.getquill.source.finagle.mysql

import java.util.Date

import scala.language.implicitConversions

import com.twitter.finagle.exp.mysql.BigDecimalValue
import com.twitter.finagle.exp.mysql.CanBeParameter
import com.twitter.finagle.exp.mysql.CanBeParameter._
import com.twitter.finagle.exp.mysql.Parameter
import com.twitter.finagle.exp.mysql.Parameter.wrap

trait FinagleMysqlEncoders {
  this: FinagleMysqlSource =>

  def encoder[T](implicit cbp: CanBeParameter[T]): Encoder[T] =
    new Encoder[T] {
      def apply(index: Int, value: T, row: List[Parameter]) =
        row :+ (value: Parameter)
    }

  implicit val stringEncoder: Encoder[String] = encoder[String]
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    new Encoder[BigDecimal] {
      def apply(index: Int, value: BigDecimal, row: List[Parameter]) =
        row :+ (BigDecimalValue(value): Parameter)
    }
  implicit val booleanEncoder: Encoder[Boolean] = encoder[Boolean]
  implicit val byteEncoder: Encoder[Byte] = encoder[Byte]
  implicit val shortEncoder: Encoder[Short] = encoder[Short]
  implicit val intEncoder: Encoder[Int] = encoder[Int]
  implicit val longEncoder: Encoder[Long] = encoder[Long]
  implicit val floatEncoder: Encoder[Float] = encoder[Float]
  implicit val doubleEncoder: Encoder[Double] = encoder[Double]
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder[Array[Byte]]
  implicit val dateEncoder: Encoder[Date] = encoder[Date]
}
