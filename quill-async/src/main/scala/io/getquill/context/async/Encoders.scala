package io.getquill.context.async

import java.util.{ Date, UUID }

import org.joda.time.LocalDateTime

trait Encoders {
  this: AsyncContext[_, _, _] =>

  def encoder[T]: Encoder[T] =
    encoder(identity[T])

  def encoder[T](f: T => Any): Encoder[T] =
    new Encoder[T] {
      def apply(index: Int, value: T, row: List[Any]) =
        row :+ f(value)
    }

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      def apply(index: Int, value: Option[T], row: List[Any]) = {
        value match {
          case None    => nullEncoder(index, null, row)
          case Some(v) => d(index, v, row)
        }
      }
    }

  private[this] val nullEncoder: Encoder[Null] = encoder[Null]

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
    encoder[Date] { (value: Date) =>
      new LocalDateTime(value)
    }
  implicit val uuidEncoder: Encoder[UUID] = encoder[UUID]
}
