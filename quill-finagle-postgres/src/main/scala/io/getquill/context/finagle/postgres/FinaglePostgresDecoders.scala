package io.getquill.context.finagle.postgres

import java.time._
import java.util.{ Date, UUID }
//import java.util.UUID

import com.twitter.finagle.postgres.values.ValueDecoder
import io.getquill.FinaglePostgresContext
import io.getquill.util.Messages.fail

import scala.reflect.{ ClassTag, classTag }

trait FinaglePostgresDecoders {
  this: FinaglePostgresContext[_] =>

  type Decoder[T] = FinaglePostgresDecoder[T]

  case class FinaglePostgresDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    override def apply(index: Index, row: ResultRow) =
      decoder(index, row)
  }

  def decoderOption[T: ClassTag](f: PartialFunction[Any, T])(implicit decoder: ValueDecoder[T]): Decoder[Option[T]] =
    FinaglePostgresDecoder[Option[T]]((index, row) => {
      row.getAnyOption(index).map(f)
      //      val value = row.vals(index).value
      //      f.lift(value).getOrElse(fail(s"Value '$value' at index $index can't be decoded to '${classTag[T].runtimeClass}'"))
    })

  def decoder[T: ClassTag, U: ClassTag](f: PartialFunction[Any, T])(implicit decoder: ValueDecoder[U]): Decoder[T] =
    FinaglePostgresDecoder[T]((index, row) => {
      row.getOption[U](index).map(f).getOrElse(fail(s"Value  at index $index can't be decoded to '${classTag[T].runtimeClass}'"))
      //      val value = row.vals(index).value
      //      f.lift(value).getOrElse(fail(s"Value '$value' at index $index can't be decoded to '${classTag[T].runtimeClass}'"))
    })

  def decoderDirectly[T: ClassTag](implicit decoder: ValueDecoder[T]): Decoder[T] =
    FinaglePostgresDecoder((index, row) =>
      row.getOption[T](index) match {
        case Some(v) => v
        case v    => fail(s"Cannot decode value $v at index $index to ${classTag[T]}")
      })

  //      implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
  //        FinaglePostgresDecoder((index, row) => {
  //          if (row.vals == null || row.vals(index) == null) None else Some(d.decoder(index, row))
  //        })

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    FinaglePostgresDecoder[Option[T]]((index, row) => {
      row.getAnyOption(index).map(_.asInstanceOf[T])
    })
  //
  //  def decoder[T](f: ResultRow => Int => T): Decoder[Option[T]] =
  //    new Decoder[Option[T]] {
  //      def apply(index: Int, row: Row): Option[T] = {
  //        // Option.apply properly handles null values
  //        Option(f(row)(index))
  //      }
  //    }

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    FinaglePostgresDecoder(mappedBaseDecoder(mapped, d.decoder))

  implicit val stringDecoder: Decoder[String] = decoderDirectly[String]
  implicit val bigDecimalDecoder: Decoder[BigDecimal] = decoderDirectly[BigDecimal]
  implicit val booleanDecoder: Decoder[Boolean] = decoderDirectly[Boolean]
  implicit val byteDecoder: Decoder[Byte] = decoder[Byte, Short] {
    case v: Short => v.toByte
  }
  implicit val shortDecoder: Decoder[Short] = decoderDirectly[Short]
  implicit val intDecoder: Decoder[Int] = decoderDirectly[Int]
  //    decoder[Int] {
  //      case v: Int  => v
  //      case v: Long => v.toInt
  //    }
  implicit val longDecoder: Decoder[Long] = decoderDirectly[Long]
  //    decoder[Long] {
  //      case v: Int  => v.toLong
  //      case v: Long => v
  //    }
  implicit val floatDecoder: Decoder[Float] = decoderDirectly[Float]
  //    decoder[Float] {
  //    case v: Double => v.toFloat
  //    case v: Float  => v
  //  }
  implicit val doubleDecoder: Decoder[Double] = decoderDirectly[Double]
  //decoder[Double] {
  //    case v: Double => v
  //    case v: Float  => v.toDouble
  //  }
  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoderDirectly[Array[Byte]]
  implicit val dateDecoder: Decoder[Date] = decoder[Date, LocalDateTime] {
    case d: LocalDateTime => Date.from(d.atZone(ZoneId.systemDefault()).toInstant);
  }
  implicit val localDateDecoder: Decoder[LocalDate] = decoder[LocalDate, LocalDate] {
    case d: LocalDateTime => d.toLocalDate
    case d: LocalDate     => d
  }
  //  implicit val localDateTimeDecoder: Decoder[LocalDateTime] = decoder[LocalDateTime] {
  //    case d: LocalDateTime => d
  //    case d: LocalDate     => d.atStartOfDay()
  //  }

  implicit val uuidDecoder: Decoder[UUID] = decoderDirectly[UUID]
}
