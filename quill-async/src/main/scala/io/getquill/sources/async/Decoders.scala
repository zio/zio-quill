package io.getquill.sources.async

import java.util.Date
import java.util.UUID

import scala.math.BigDecimal.javaBigDecimal2bigDecimal
import scala.reflect.ClassTag
import scala.reflect.classTag

import org.joda.time.LocalDate
import org.joda.time.LocalDateTime

import com.github.mauricio.async.db.RowData

import io.getquill.util.Messages.fail

trait Decoders {
  this: AsyncSource[_, _, _] =>

  def decoder[T: ClassTag](f: PartialFunction[Any, T] = PartialFunction.empty): Decoder[T] =
    new Decoder[T] {
      def apply(index: Int, row: RowData) = {
        row(index) match {
          case value: T                        => value
          case value if (f.isDefinedAt(value)) => f(value)
          case value =>
            fail(s"Value '$value' of ${value.getClass.getName} can't be decoded to '${classTag[T].runtimeClass}'")
        }
      }
    }

  trait NumericDecoder[T] extends Decoder[T] {
    def apply(index: Int, row: RowData) =
      row(index) match {
        case v: Byte       => decode(v)
        case v: Short      => decode(v)
        case v: Int        => decode(v)
        case v: Long       => decode(v)
        case v: Float      => decode(v)
        case v: Double     => decode(v)
        case v: BigDecimal => decode(v)
        case other =>
          fail(s"Value $other is not numeric")
      }
    def decode[U](v: U)(implicit n: Numeric[U]): T
  }

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    new Decoder[Option[T]] {
      def apply(index: Int, row: RowData) = {
        row(index) match {
          case null  => None
          case value => Some(d(index, row))
        }
      }
    }

  implicit val stringDecoder: Decoder[String] = decoder[String]()

  implicit val bigDecimalDecoder: Decoder[BigDecimal] =
    new NumericDecoder[BigDecimal] {
      def decode[U](v: U)(implicit n: Numeric[U]) =
        BigDecimal(n.toDouble(v))
    }

  implicit val booleanDecoder: Decoder[Boolean] = decoder[Boolean] {
    case v: Byte         => v == (1: Byte)
    case v: Short        => v == (1: Short)
    case v: Int          => v == 1
    case v: Long         => v == 1L
  }

  implicit val byteDecoder: Decoder[Byte] = decoder[Byte] {
    case v: Short => v.toByte
  }

  implicit val shortDecoder: Decoder[Short] = decoder[Short] {
    case v: Byte => v.toShort
  }

  implicit val intDecoder: Decoder[Int] =
    new NumericDecoder[Int] {
      def decode[U](v: U)(implicit n: Numeric[U]) =
        n.toInt(v)
    }

  implicit val longDecoder: Decoder[Long] =
    new NumericDecoder[Long] {
      def decode[U](v: U)(implicit n: Numeric[U]) =
        n.toLong(v)
    }

  implicit val floatDecoder: Decoder[Float] =
    new NumericDecoder[Float] {
      def decode[U](v: U)(implicit n: Numeric[U]) =
        n.toFloat(v)
    }

  implicit val doubleDecoder: Decoder[Double] =
    new NumericDecoder[Double] {
      def decode[U](v: U)(implicit n: Numeric[U]) =
        n.toDouble(v)
    }

  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoder[Array[Byte]]()

  implicit val dateDecoder: Decoder[Date] =
    decoder[Date] {
      case localDateTime: LocalDateTime =>
        localDateTime.toDate
      case localDate: LocalDate =>
        localDate.toDate
    }

  implicit val uuidDecoder: Decoder[UUID] = new Decoder[UUID] {
    def apply(index: Int, row: RowData): UUID = row(index) match {
      case value: UUID => value
    }
  }

}
