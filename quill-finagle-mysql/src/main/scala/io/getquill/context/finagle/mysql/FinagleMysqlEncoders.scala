package io.getquill.context.finagle.mysql

import java.sql.Timestamp
import java.time.{ LocalDate, LocalDateTime }
import java.util.Date

import com.twitter.finagle.mysql._
import com.twitter.finagle.mysql.CanBeParameter._
import com.twitter.finagle.mysql.Parameter.wrap
import io.getquill.FinagleMysqlContext

trait FinagleMysqlEncoders {
  this: FinagleMysqlContext[_] =>

  def encoder[T](implicit cbp: CanBeParameter[T]): Encoder[T] =
    encoder[T]((v: T) => v: Parameter)

  def encoder[T](f: T => Parameter): Encoder[T] =
    new Encoder[T] {
      def apply(idx: Int, value: T, row: List[Parameter]) = {
        row :+ f(value)
      }
    }

  private[this] val nullEncoder = encoder((_: Null) => Parameter.NullParameter)

  implicit def optionEncoder[T](implicit e: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      def apply(idx: Int, value: Option[T], row: List[Parameter]) =
        value match {
          case None        => nullEncoder(idx, null, row)
          case Some(value) => e(idx, value, row)
        }
    }

  implicit val stringEncoder: Encoder[String] = encoder[String]
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    encoder[BigDecimal] { (value: BigDecimal) =>
      BigDecimalValue(value): Parameter
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
  implicit val localDateEncoder: Encoder[LocalDate] = encoder[LocalDate] {
    (d: LocalDate) => DateValue(java.sql.Date.valueOf(d)): Parameter
  }
  implicit val localDateTimeEncoder: Encoder[LocalDateTime] = encoder[LocalDateTime] {
    (d: LocalDateTime) => new TimestampValue(dateTimezone, dateTimezone).apply(Timestamp.valueOf(d)): Parameter
  }
}
