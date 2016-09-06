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

  protected case class ValueEncoderEncoder[T](encoder: ValueEncoder[T]) extends Encoder[T] {
    def apply(idx: Int, value: T, row: PrepareRow) = {
      row :+ Param(value)(encoder)
    }
  }

  protected def encoder[T](implicit e: ValueEncoder[T]): Encoder[T] = new ValueEncoderEncoder(e)
  protected def encoder[T, U](f: U => T)(implicit e: ValueEncoder[T]): Encoder[U] = new ValueEncoderEncoder[U](e.contraMap(f))

  override protected def mappedEncoderImpl[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    e match {
      case v: ValueEncoderEncoder[O] => encoder[O, I](mapped.f)(v.encoder)
    }

  implicit def optionEncoder[T](implicit e: Encoder[T]): Encoder[Option[T]] = e match {
    case v: ValueEncoderEncoder[T] => encoder[Option[T]](option(v.encoder))
  }

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
  implicit val dateEncoder: Encoder[Date] = encoder[LocalDateTime, Date]((v: Date) => LocalDateTime.ofInstant(v.toInstant(), ZoneId.systemDefault()))
  implicit val uuidEncoder: Encoder[UUID] = encoder[UUID]
}
