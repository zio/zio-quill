package io.getquill.context.zio.jasync

import io.getquill.context.sql.encoding.ArrayEncoding
import io.getquill.context.zio.{PostgresZioJAsyncContext, SqlTypes}
import io.getquill.util.Messages.fail

import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.util
import java.util.Date
import scala.collection.compat._
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

trait ArrayDecoders extends ArrayEncoding {
  self: PostgresZioJAsyncContext[_] =>

  implicit def arrayStringDecoder[Col <: Seq[String]](implicit bf: CBF[String, Col]): Decoder[Col] =
    arrayRawDecoder[String, Col]
  implicit def arrayBigDecimalDecoder[Col <: Seq[BigDecimal]](implicit bf: CBF[BigDecimal, Col]): Decoder[Col] =
    arrayDecoder[java.math.BigDecimal, BigDecimal, Col](BigDecimal.javaBigDecimal2bigDecimal)
  implicit def arrayBooleanDecoder[Col <: Seq[Boolean]](implicit bf: CBF[Boolean, Col]): Decoder[Col] =
    arrayRawDecoder[Boolean, Col]
  implicit def arrayByteDecoder[Col <: Seq[Byte]](implicit bf: CBF[Byte, Col]): Decoder[Col] =
    arrayDecoder[Short, Byte, Col](_.toByte)
  implicit def arrayShortDecoder[Col <: Seq[Short]](implicit bf: CBF[Short, Col]): Decoder[Col] =
    arrayRawDecoder[Short, Col]
  implicit def arrayIntDecoder[Col <: Seq[Index]](implicit bf: CBF[Index, Col]): Decoder[Col] =
    arrayRawDecoder[Index, Col]
  implicit def arrayLongDecoder[Col <: Seq[Long]](implicit bf: CBF[Long, Col]): Decoder[Col] =
    arrayRawDecoder[Long, Col]
  implicit def arrayFloatDecoder[Col <: Seq[Float]](implicit bf: CBF[Float, Col]): Decoder[Col] =
    arrayDecoder[Double, Float, Col](_.toFloat)
  implicit def arrayDoubleDecoder[Col <: Seq[Double]](implicit bf: CBF[Double, Col]): Decoder[Col] =
    arrayRawDecoder[Double, Col]
  implicit def arrayDateDecoder[Col <: Seq[Date]](implicit bf: CBF[Date, Col]): Decoder[Col] =
    arrayDecoder[LocalDateTime, Date, Col](d => Date.from(d.atZone(ZoneId.systemDefault()).toInstant))
  implicit def arrayLocalDateDecoder[Col <: Seq[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col] =
    arrayRawDecoder[LocalDate, Col]
  implicit def arrayLocalDateTimeDecoder[Col <: Seq[LocalDateTime]](implicit
    bf: CBF[LocalDateTime, Col]
  ): Decoder[Col] = arrayRawDecoder[LocalDateTime, Col]

  def arrayDecoder[I, O, Col <: Seq[O]](
    mapper: I => O
  )(implicit bf: CBF[O, Col], iTag: ClassTag[I], oTag: ClassTag[O]): Decoder[Col] =
    AsyncDecoder[Col](SqlTypes.ARRAY)(new BaseDecoder[Col] {
      def apply(index: Index, row: ResultRow, session: Session): Col = row.get(index) match {
        case seq: util.ArrayList[_] =>
          seq.asScala
            .foldLeft(bf.newBuilder) {
              case (b, x: I) => b += mapper(x)
              case (_, x) =>
                fail(s"Array at index $index contains element of ${x.getClass.getCanonicalName}, but expected $iTag")
            }
            .result()
        case value =>
          fail(
            s"Value '$value' at index $index is not an array so it cannot be decoded to collection of $oTag"
          )
      }
    })

  def arrayRawDecoder[T: ClassTag, Col <: Seq[T]](implicit bf: CBF[T, Col]): Decoder[Col] =
    arrayDecoder[T, T, Col](identity)
}
