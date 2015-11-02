package io.getquill.source.async.postgresql

import com.github.mauricio.async.db.RowData
import io.getquill.util.Messages._
import io.getquill.source.sql.SqlSource
import io.getquill.source.sql.idiom.MySQLDialect
import io.getquill.source.sql.naming.NamingStrategy
import scala.reflect.ClassTag
import scala.reflect.classTag
import org.joda.time.LocalDateTime
import org.joda.time.DateTimeZone
import org.joda.time.DateTime
import java.util.Date

trait PostgresqlAsyncDecoders {
  this: PostgresqlAsyncSource[_] =>

  def decoder[T: ClassTag](f: PartialFunction[Any, T] = PartialFunction.empty): Decoder[T] =
    new Decoder[T] {
      def apply(index: Int, row: RowData) = {
        row(index) match {
          case value: T                        => value
          case value if (f.isDefinedAt(value)) => f(value)
          case value                           => fail(s"Value '$value' can't be decoded to '${classTag[T].runtimeClass}'")
        }
      }
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

  implicit val bigDecimalDecoder: Decoder[BigDecimal] = decoder[BigDecimal] {
    case v: java.math.BigDecimal => v
  }

  implicit val booleanDecoder: Decoder[Boolean] = decoder[Boolean] {
    case byte: Byte => byte == (1: Byte)
  }

  implicit val byteDecoder: Decoder[Byte] = decoder[Byte] {
    case v: Short => v.toByte
  }

  implicit val shortDecoder: Decoder[Short] = decoder[Short]()

  implicit val intDecoder: Decoder[Int] = decoder[Int] {
    case v: Long => v.toInt
  }

  implicit val longDecoder: Decoder[Long] = decoder[Long] {
    case localDateTime: LocalDateTime =>
      localDateTime.toDateTime(DateTimeZone.UTC).getMillis / 1000
    case dateTime: DateTime =>
      dateTime.getMillis / 1000
  }

  implicit val floatDecoder: Decoder[Float] = decoder[Float] {
    case n => n.toString.toFloat
  }

  implicit val doubleDecoder: Decoder[Double] = decoder[Double] {
    case n => n.toString.toDouble
  }

  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoder[Array[Byte]]()

  implicit val dateDecoder: Decoder[Date] =
    decoder[Date] {
      case localDateTime: LocalDateTime =>
        localDateTime.toDateTime(DateTimeZone.UTC).toDate
      case dateTime: DateTime =>
        dateTime.toDate
    }
}
