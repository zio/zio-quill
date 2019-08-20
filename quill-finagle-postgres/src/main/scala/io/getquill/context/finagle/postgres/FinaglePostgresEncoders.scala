package io.getquill.context.finagle.postgres

import com.twitter.finagle.postgres._
import com.twitter.finagle.postgres.values._
import com.twitter.finagle.postgres.values.ValueEncoder._
import io.getquill.FinaglePostgresContext
import java.util.{ Date, UUID }
import java.time._

trait FinaglePostgresEncoders {
  this: FinaglePostgresContext[_] =>

  type Encoder[T] = FinaglePostgresEncoder[T]

  case class FinaglePostgresEncoder[T](encoder: ValueEncoder[T]) extends BaseEncoder[T] {
    override def apply(index: Index, value: T, row: PrepareRow) =
      row :+ Param(value)(encoder)
  }

  def encoder[T](implicit e: ValueEncoder[T]): Encoder[T] = FinaglePostgresEncoder(e)

  def encoder[T, U](f: U => T)(implicit e: ValueEncoder[T]): Encoder[U] =
    encoder[U](e.contraMap(f))

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    FinaglePostgresEncoder(e.encoder.contraMap(mapped.f))

  implicit def optionEncoder[T](implicit e: Encoder[T]): Encoder[Option[T]] =
    FinaglePostgresEncoder[Option[T]](option(e.encoder))

  implicit val stringEncoder: Encoder[String] = encoder[String]
  implicit val bigDecimalEncoder: Encoder[BigDecimal] = encoder[BigDecimal]
  implicit val booleanEncoder: Encoder[Boolean] = encoder[Boolean]
  implicit val byteEncoder: Encoder[Byte] = encoder[Short, Byte](_.toShort)
  implicit val shortEncoder: Encoder[Short] = encoder[Short]
  implicit val intEncoder: Encoder[Int] = encoder[Int]
  implicit val longEncoder: Encoder[Long] = encoder[Long]
  implicit val floatEncoder: Encoder[Float] = encoder[Float]
  implicit val doubleEncoder: Encoder[Double] = encoder[Double]
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder[Array[Byte]](bytea)
  implicit val dateEncoder: Encoder[Date] =
    encoder[LocalDateTime, Date]((v: Date) => LocalDateTime.ofInstant(v.toInstant, ZoneId.systemDefault()))
  implicit val localDateEncoder: Encoder[LocalDate] = encoder[LocalDate]
  implicit val localDateTimeEncoder: Encoder[LocalDateTime] = encoder[LocalDateTime]
  implicit val uuidEncoder: Encoder[UUID] = encoder[UUID]
}
