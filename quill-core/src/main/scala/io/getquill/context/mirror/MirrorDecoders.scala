package io.getquill.context.mirror

import java.time.LocalDate
import java.util.Date

import io.getquill.context.Context

import scala.reflect.ClassTag

trait MirrorDecoders {
  this: Context[_, _] =>

  type ResultRow = Row

  def decoder[T: ClassTag]: Decoder[T] = new Decoder[T] {
    def apply(index: Int, row: Row) =
      row[T](index)
  }

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    new Decoder[Option[T]] {
      def apply(index: Int, row: Row) =
        row[Option[Any]](index) match {
          case Some(v) => Some(d(0, Row(v)))
          case None    => None
        }
    }

  implicit val stringDecoder: Decoder[String] = decoder[String]
  implicit val bigDecimalDecoder: Decoder[BigDecimal] = decoder[BigDecimal]
  implicit val booleanDecoder: Decoder[Boolean] = decoder[Boolean]
  implicit val byteDecoder: Decoder[Byte] = decoder[Byte]
  implicit val shortDecoder: Decoder[Short] = decoder[Short]
  implicit val intDecoder: Decoder[Int] = decoder[Int]
  implicit val longDecoder: Decoder[Long] = decoder[Long]
  implicit val floatDecoder: Decoder[Float] = decoder[Float]
  implicit val doubleDecoder: Decoder[Double] = decoder[Double]
  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoder[Array[Byte]]
  implicit val dateDecoder: Decoder[Date] = decoder[Date]
  implicit val localDateDecoder: Decoder[LocalDate] = decoder[LocalDate]
}
