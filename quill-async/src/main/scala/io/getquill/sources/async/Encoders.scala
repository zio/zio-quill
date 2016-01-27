package io.getquill.sources.async

import java.util.Date
import java.util.UUID

import org.joda.time.LocalDateTime

trait Encoders {
  this: AsyncSource[_, _, _] =>

  def encoder[T]: Encoder[T] =
    new Encoder[T] {
      def apply(index: Int, value: T, row: List[Any]) =
        row :+ value
    }

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      def apply(index: Int, value: Option[T], row: List[Any]) =
        row :+ (value match {
          case None        => null
          case Some(value) => value
        })
    }

  implicit val stringEncoder: Encoder[String] = encoder[String]
  implicit val bigDecimalEncoder: Encoder[BigDecimal] = encoder[BigDecimal]
  implicit val booleanEncoder: Encoder[Boolean] = encoder[Boolean]
  implicit val byteEncoder: Encoder[Byte] = encoder[Byte]
  implicit val shortEncoder: Encoder[Short] = encoder[Short]
  implicit val intEncoder: Encoder[Int] = encoder[Int]
  implicit val longEncoder: Encoder[Long] = encoder[Long]
  implicit val floatEncoder: Encoder[Float] = encoder[Float]
  implicit val doubleEncoder: Encoder[Double] = encoder[Double]
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder[Array[Byte]]
  implicit val dateEncoder: Encoder[Date] =
    new Encoder[Date] {
      def apply(index: Int, value: Date, row: List[Any]) =
        row :+ new LocalDateTime(value)
    }

  implicit val uuidEncoder: Encoder[UUID] =
    new Encoder[UUID] {
      def apply(index: Int, value: UUID, row: List[Any]) =
        row :+ value
    }
}
