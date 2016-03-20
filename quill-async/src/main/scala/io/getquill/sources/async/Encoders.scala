package io.getquill.sources.async

import java.util.Date
import java.util.UUID
import org.joda.time.LocalDateTime
import io.getquill.sources.BindedStatementBuilder

trait Encoders {
  this: AsyncSource[_, _, _] =>

  def encoder[T]: Encoder[T] =
    encoder(identity[T] _)

  def encoder[T](f: T => Any): Encoder[T] =
    new Encoder[T] {
      def apply(index: Int, value: T, row: BindedStatementBuilder[List[Any]]) = {
        val raw = new io.getquill.sources.Encoder[List[Any], T] {
          override def apply(index: Int, value: T, row: List[Any]) =
            row :+ value
        }
        row.single(index, value, raw)
      }
    }

  implicit def traversableEncoder[T](implicit e: Encoder[T]): Encoder[Traversable[T]] =
    new Encoder[Traversable[T]] {
      def apply(index: Int, values: Traversable[T], row: BindedStatementBuilder[List[Any]]) =
        row.coll[T](index, values, e)
    }

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    encoder[Option[T]] { (value: Option[T]) =>
      value match {
        case None        => null
        case Some(value) => value
      }
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
    encoder[Date] { (value: Date) =>
      new LocalDateTime(value)
    }
  implicit val uuidEncoder: Encoder[UUID] = encoder[UUID]
}
