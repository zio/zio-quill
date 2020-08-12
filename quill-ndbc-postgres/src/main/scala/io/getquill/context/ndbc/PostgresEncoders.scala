package io.getquill.context.ndbc

import java.time.{ LocalDate, LocalDateTime, ZoneOffset }
import java.util.{ Date, UUID }

import io.getquill.dsl.CoreDsl
import io.trane.ndbc.PostgresPreparedStatement

import scala.language.implicitConversions
import scala.reflect.ClassTag

trait LowPriorityPostgresImplicits {
  this: CoreDsl =>

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: BaseEncoder[O]): BaseEncoder[I] =
    mappedBaseEncoder(mapped, e)
}

trait PostgresEncoders extends LowPriorityPostgresImplicits with io.getquill.dsl.LowPriorityImplicits {
  this: NdbcContextBase[_, _, PostgresPreparedStatement, _] =>

  type Encoder[T] = BaseEncoder[T]

  protected val zoneOffset: ZoneOffset

  def encoder[T, U](f: PostgresPreparedStatement => (Int, U) => PostgresPreparedStatement)(implicit ev: T => U): Encoder[T] =
    (idx, v, ps) =>
      if (v == null) ps.setNull(idx)
      else f(ps)(idx, v)

  def arrayEncoder[T, U: ClassTag, Col <: Seq[T]](f: PostgresPreparedStatement => (Int, Array[U]) => PostgresPreparedStatement)(ev: T => U): Encoder[Col] =
    (idx, v, ps) =>
      if (v == null) ps.setNull(idx)
      else f(ps)(idx, v.map(ev).toArray[U])

  implicit override def anyValMappedEncoder[I <: AnyVal, O](implicit mapped: MappedEncoding[I, O], encoder: Encoder[O]): Encoder[I] = mappedEncoder

  implicit def optionEncoder[T](implicit e: Encoder[T]): Encoder[Option[T]] =
    (idx, v, ps) =>
      if (v == null) ps.setNull(idx)
      else v match {
        case None    => ps.setNull(idx)
        case Some(v) => e(idx, v, ps)
      }

  implicit def toLocalDateTime(d: Date) = LocalDateTime.ofInstant(d.toInstant(), zoneOffset)

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
  implicit val dateEncoder: Encoder[Date] = encoder(_.setLocalDateTime)
  implicit val localDateEncoder: Encoder[LocalDate] = encoder(_.setLocalDate)
  implicit val localDateTimeEncoder: Encoder[LocalDateTime] = encoder(_.setLocalDateTime)

  implicit def arrayStringEncoder[Col <: Seq[String]]: Encoder[Col] = arrayEncoder[String, String, Col](_.setStringArray)(identity)
  implicit def arrayBigDecimalEncoder[Col <: Seq[BigDecimal]]: Encoder[Col] = arrayEncoder[BigDecimal, java.math.BigDecimal, Col](_.setBigDecimalArray)(_.bigDecimal)
  implicit def arrayBooleanEncoder[Col <: Seq[Boolean]]: Encoder[Col] = arrayEncoder[Boolean, java.lang.Boolean, Col](_.setBooleanArray)(_.booleanValue)
  implicit def arrayByteEncoder[Col <: Seq[Byte]]: Encoder[Col] = arrayEncoder[Byte, java.lang.Short, Col](_.setShortArray)(identity)
  implicit def arrayShortEncoder[Col <: Seq[Short]]: Encoder[Col] = arrayEncoder[Short, java.lang.Short, Col](_.setShortArray)(_.shortValue)
  implicit def arrayIntEncoder[Col <: Seq[Int]]: Encoder[Col] = arrayEncoder[Int, java.lang.Integer, Col](_.setIntegerArray)(_.intValue)
  implicit def arrayLongEncoder[Col <: Seq[Long]]: Encoder[Col] = arrayEncoder[Long, java.lang.Long, Col](_.setLongArray)(_.longValue)
  implicit def arrayFloatEncoder[Col <: Seq[Float]]: Encoder[Col] = arrayEncoder[Float, java.lang.Float, Col](_.setFloatArray)(_.floatValue)
  implicit def arrayDoubleEncoder[Col <: Seq[Double]]: Encoder[Col] = arrayEncoder[Double, java.lang.Double, Col](_.setDoubleArray)(_.doubleValue)
  implicit def arrayDateEncoder[Col <: Seq[Date]]: Encoder[Col] = arrayEncoder[Date, LocalDateTime, Col](_.setLocalDateTimeArray)(identity)
  implicit def arrayLocalDateEncoder[Col <: Seq[LocalDate]]: Encoder[Col] = arrayEncoder[LocalDate, LocalDate, Col](_.setLocalDateArray)(identity)
}
