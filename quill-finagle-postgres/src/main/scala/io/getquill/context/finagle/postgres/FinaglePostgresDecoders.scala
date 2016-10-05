package io.getquill.context.finagle.postgres

import com.twitter.finagle.postgres._
import io.getquill.FinaglePostgresContext
import io.getquill.util.Messages.fail
import java.time._
import java.util.{ Date, UUID }
import scala.reflect.ClassTag
import scala.reflect.classTag

trait FinaglePostgresDecoders {
  this: FinaglePostgresContext[_] =>

  def decoder[T: ClassTag](f: PartialFunction[Any, T]): Decoder[T] = new Decoder[T] {
    def apply(idx: Int, row: Row) = {
      val value = row.vals(idx).value
      f.lift(value).getOrElse(fail(s"Value '$value' at index $idx can't be decoded to '${classTag[T].runtimeClass}'"))
    }
  }

  def decoderDirectly[T: ClassTag] = new Decoder[T] {
    def apply(idx: Int, row: Row) = row.vals(idx).value match {
      case v: T => v
      case v    => fail(s"Cannot decode value ${v} at index $idx to ${classTag[T]}")
    }
  }

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] = new Decoder[Option[T]] {
    def apply(idx: Int, row: Row) = {
      if (row.vals == null || row.vals(idx) == null) None else Some(d.apply(idx, row))
    }
  }

  implicit val stringDecoder: Decoder[String] = decoderDirectly[String]
  implicit val bigDecimalDecoder: Decoder[BigDecimal] = decoderDirectly[BigDecimal]
  implicit val booleanDecoder: Decoder[Boolean] = decoderDirectly[Boolean]
  implicit val byteDecoder: Decoder[Byte] = decoder[Byte] {
    case v: Short => v.toByte
  }
  implicit val shortDecoder: Decoder[Short] = decoderDirectly[Short]
  implicit val intDecoder: Decoder[Int] =
    decoder[Int] {
      case v: Int  => v
      case v: Long => v.toInt
    }
  implicit val longDecoder: Decoder[Long] =
    decoder[Long] {
      case v: Int  => v.toLong
      case v: Long => v
    }
  implicit val floatDecoder: Decoder[Float] = decoder[Float] {
    case v: Double => v.toFloat
    case v: Float  => v
  }
  implicit val doubleDecoder: Decoder[Double] = decoder[Double] {
    case v: Double => v
    case v: Float  => v.toDouble
  }
  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoderDirectly[Array[Byte]]
  implicit val dateDecoder: Decoder[Date] =
    decoder[Date] {
      case d: LocalDateTime => Date.from(d.atZone(ZoneId.systemDefault()).toInstant());
    }
  implicit val localDateDecoder: Decoder[LocalDate] = decoder[LocalDate] {
    case d: LocalDateTime => d.toLocalDate
    case d: LocalDate     => d
  }
  implicit val localDateTimeDecoder: Decoder[LocalDateTime] = decoder[LocalDateTime] {
    case d: LocalDateTime => d
    case d: LocalDate     => d.atStartOfDay()
  }

  implicit val uuidDecoder: Decoder[UUID] = decoderDirectly[UUID]
}
