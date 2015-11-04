package io.getquill.source.async

import java.util.Date

import org.joda.time.LocalDateTime

trait Encoders {
  this: AsyncSource[_, _, _] =>

  def encoder[T](f: T => Any): Encoder[T] =
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

  implicit val stringEncoder: Encoder[String] = encoder[String](identity)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] = encoder[BigDecimal](identity)
  implicit val booleanEncoder: Encoder[Boolean] = encoder[Boolean](identity)
  implicit val byteEncoder: Encoder[Byte] = encoder[Byte](identity)
  implicit val shortEncoder: Encoder[Short] = encoder[Short](identity)
  implicit val intEncoder: Encoder[Int] = encoder[Int](identity)
  implicit val longEncoder: Encoder[Long] = encoder[Long](identity)
  implicit val floatEncoder: Encoder[Float] = encoder[Float](identity)
  implicit val doubleEncoder: Encoder[Double] = encoder[Double](identity)
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder[Array[Byte]](identity)
  implicit val dateEncoder: Encoder[Date] = encoder[Date](new LocalDateTime(_))
}
