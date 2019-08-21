package io.getquill.context.finagle.postgres

import java.time.{ LocalDate, LocalDateTime, ZoneId }
import java.util.{ Date, UUID }

import com.twitter.finagle.postgres.values.ValueDecoder
import io.getquill.FinaglePostgresContext
import io.getquill.util.Messages.fail

import scala.reflect.{ ClassTag, classTag }

trait FinaglePostgresDecoders {
  this: FinaglePostgresContext[_] =>

  import ValueDecoder._

  type Decoder[T] = FinaglePostgresDecoder[T]

  case class FinaglePostgresDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    override def apply(index: Index, row: ResultRow): T =
      decoder(index, row)
  }

  def decoder[T: ClassTag](f: PartialFunction[Any, T]): Decoder[T] =
    FinaglePostgresDecoder((index, row) => {
      row.getAnyOption(index) match {
        case Some(v: T)                  => v
        case Some(v) if f.isDefinedAt(v) => f(v)
        case v                           => fail(s"Cannot decode value $v at index $index to ${classTag[T]}")
      }
    })

  implicit def decoderDirectly[T: ClassTag](implicit vd: ValueDecoder[T]): Decoder[T] =
    FinaglePostgresDecoder((index, row) =>
      row.get[T](index) match {
        case v: T => v
        case v    => fail(s"Cannot decode value $v at index $index to ${classTag[T]}")
      })

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    FinaglePostgresDecoder((index, row) => {
      row.getAnyOption(index).map(_ => d.decoder(index, row))
    })

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    FinaglePostgresDecoder(mappedBaseDecoder(mapped, d.decoder))

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
  implicit val doubleDecoder: Decoder[Double] = decoderDirectly[Double]
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