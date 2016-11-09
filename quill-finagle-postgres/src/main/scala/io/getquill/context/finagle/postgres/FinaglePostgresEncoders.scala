package io.getquill.context.finagle.postgres

import com.twitter.finagle.postgres._
import com.twitter.finagle.postgres.values._
import com.twitter.finagle.postgres.values.ValueEncoder._
import io.getquill.FinaglePostgresContext
import java.util.{ Date, UUID }
import java.time._
import org.jboss.netty.buffer.ChannelBuffers

trait FinaglePostgresEncoders {
  this: FinaglePostgresContext[_] =>

  type Encoder[T] = FinanglePostgresEncoder[T]

  case class FinanglePostgresEncoder[T](encoder: ValueEncoder[T]) extends BaseEncoder[T] {
    override def apply(index: Index, value: T, row: PrepareRow) =
      row :+ Param(value)(encoder)
  }

  def encoder[T](implicit e: ValueEncoder[T]): Encoder[T] = FinanglePostgresEncoder(e)

  def encoder[T, U](f: U => T)(implicit e: ValueEncoder[T]): Encoder[U] =
    encoder[U](e.contraMap(f))

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    FinanglePostgresEncoder(e.encoder.contraMap(mapped.f))

  implicit def optionEncoder[T](implicit e: Encoder[T]): Encoder[Option[T]] =
    FinanglePostgresEncoder[Option[T]](option(e.encoder))

  //Workaround for https://github.com/finagle/finagle-postgres/pull/28
  implicit val bytea: ValueEncoder[Array[Byte]] = instance(
    "bytea",
    bytes => "\\x" + bytes.map("%02x".format(_)).mkString,
    (b, c) => Some(ChannelBuffers.copiedBuffer(b))
  )

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
