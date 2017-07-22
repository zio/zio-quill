package io.getquill.context.ndbc

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

import io.trane.ndbc.PreparedStatement
import java.util.UUID

trait StandardEncoders {
  this: BaseNdbcContext[_, _, PreparedStatement, _] =>

  type Encoder[T] = BaseEncoder[T]

  protected val zoneOffset: ZoneOffset

  def encoder[T, U](f: PreparedStatement => (Int, U) => PreparedStatement)(implicit ev: T => U): Encoder[T] =
    (idx, v, ps) =>
      if (v == null) ps.setNull(idx)
      else f(ps)(idx, v)

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    mappedBaseEncoder(mapped, e)

  implicit def optionEncoder[T](implicit e: Encoder[T]): Encoder[Option[T]] =
    (idx, v, ps) =>
      v match {
        case None    => ps.setNull(idx)
        case Some(v) => e(idx, v, ps)
      }

  implicit val uuidEncoder: Encoder[UUID] = encoder(_.setUUID)
  implicit val stringEncoder: Encoder[String] = encoder(_.setString)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] = encoder(_.setBigDecimal)(_.bigDecimal)
  implicit val booleanEncoder: Encoder[Boolean] = encoder(_.setBoolean)
  implicit val byteEncoder: Encoder[Byte] = encoder(_.setByte)
  implicit val shortEncoder: Encoder[Short] = encoder(_.setShort)
  implicit val intEncoder: Encoder[Int] = encoder(_.setInteger)
  implicit val longEncoder: Encoder[Long] = encoder(_.setLong)
  implicit val floatEncoder: Encoder[Float] = encoder(_.setFloat)
  implicit val doubleEncoder: Encoder[Double] = encoder(_.setDouble)
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder(_.setByteArray)
  implicit val dateEncoder: Encoder[Date] = encoder(_.setLocalDateTime)(d => LocalDateTime.ofInstant(d.toInstant(), zoneOffset))
  implicit val localDateEncoder: Encoder[LocalDate] = encoder(_.setLocalDate)
  implicit val localDateTimeEncoder: Encoder[LocalDateTime] = encoder(_.setLocalDateTime)
}