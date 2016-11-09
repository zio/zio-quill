package io.getquill.context.finagle.postgres

import java.time._
import java.util.{ Date, UUID }

import io.getquill.FinaglePostgresContext
import io.getquill.util.Messages.fail

import scala.reflect.{ ClassTag, classTag }

trait FinaglePostgresDecoders {
  this: FinaglePostgresContext[_] =>

  type Decoder[T] = FinanglePostgresDecoder[T]

  case class FinanglePostgresDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    override def apply(index: Index, row: ResultRow) =
      decoder(index, row)
  }

  def decoder[T: ClassTag](f: PartialFunction[Any, T]): Decoder[T] =
    FinanglePostgresDecoder((index, row) => {
      val value = row.vals(index).value
      f.lift(value).getOrElse(fail(s"Value '$value' at index $index can't be decoded to '${classTag[T].runtimeClass}'"))
    })

  def decoderDirectly[T: ClassTag]: Decoder[T] =
    FinanglePostgresDecoder((index, row) =>
      row.vals(index).value match {
        case v: T => v
        case v    => fail(s"Cannot decode value $v at index $index to ${classTag[T]}")
      })

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    FinanglePostgresDecoder((index, row) => {
      if (row.vals == null || row.vals(index) == null) None else Some(d.decoder(index, row))
    })

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    FinanglePostgresDecoder(mappedBaseDecoder(mapped, d.decoder))

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
      case d: LocalDateTime => Date.from(d.atZone(ZoneId.systemDefault()).toInstant);
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
