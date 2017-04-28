package io.getquill.context.jdbc

import java.sql.{Timestamp, Types}
import java.time.LocalDate
import java.util.Date
import java.sql.{Date => SqlDate}
import java.math.{BigDecimal => JBigDecimal}

import io.getquill.context.sql.encoding.ArrayEncoding
import io.getquill.util.Messages.fail

import scala.collection.generic.CanBuildFrom
import scala.reflect.ClassTag

trait ArrayDecoders extends ArrayEncoding {
  self: JdbcContext[_, _] =>

  implicit def arrayStringDecoder[Col <: Seq[String]](implicit bf: CanBuildFrom[Nothing, String, Col]): Decoder[Col] = rawDecoder[String, Col]
  implicit def arrayBigDecimalDecoder[Col <: Seq[BigDecimal]](implicit bf: CBF[BigDecimal, Col]): Decoder[Col] = arrayDecoder[JBigDecimal, BigDecimal, Col](BigDecimal.apply)
  implicit def arrayBooleanDecoder[Col <: Seq[Boolean]](implicit bf: CBF[Boolean, Col]): Decoder[Col] = rawDecoder[Boolean, Col]
  implicit def arrayByteDecoder[Col <: Seq[Byte]](implicit bf: CBF[Byte, Col]): Decoder[Col] = rawDecoder[Byte, Col]
  implicit def arrayShortDecoder[Col <: Seq[Short]](implicit bf: CBF[Short, Col]): Decoder[Col] = rawDecoder[Short, Col]
  implicit def arrayIntDecoder[Col <: Seq[Int]](implicit bf: CBF[Int, Col]): Decoder[Col] = rawDecoder[Int, Col]
  implicit def arrayLongDecoder[Col <: Seq[Long]](implicit bf: CBF[Long, Col]): Decoder[Col] = rawDecoder[Long, Col]
  implicit def arrayFloatDecoder[Col <: Seq[Float]](implicit bf: CBF[Float, Col]): Decoder[Col] = rawDecoder[Float, Col]
  implicit def arrayDoubleDecoder[Col <: Seq[Double]](implicit bf: CBF[Double, Col]): Decoder[Col] = rawDecoder[Double, Col]
  implicit def arrayDateDecoder[Col <: Seq[Date]](implicit bf: CBF[Date, Col]): Decoder[Col] = rawDecoder[Date, Col]
  implicit def arrayTimestampDecoder[Col <: Seq[Timestamp]](implicit bf: CBF[Timestamp, Col]): Decoder[Col] = rawDecoder[Timestamp, Col]
  implicit def arrayLocalDateDecoder[Col <: Seq[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col] = arrayDecoder[SqlDate, LocalDate, Col](_.toLocalDate)

  /**
   * Generic encoder for JDBC arrays.
   *
   * @param mapper retrieved raw types fro JDBC array may be mapped via this mapper to satisfy encoder type
   * @param bf builder factory is needed to create instances of decoder's collection
   * @tparam I raw type retrieved form JDBC array
   * @tparam O mapped type fulfilled in decoder's collection
   * @tparam Col type of decoder's collection
   * @return JDBC array decoder
   */
  def arrayDecoder[I, O, Col <: Seq[O]](mapper: I => O)(implicit bf: CanBuildFrom[Nothing, O, Col], tag: ClassTag[I]): Decoder[Col] = {
    decoder[Col](Types.ARRAY, (idx: Index, row: ResultRow) => {
      val arr = row.getArray(idx)
      if (arr == null) bf().result()
      else {
        arr.getArray.asInstanceOf[Array[AnyRef]].foldLeft(bf()) {
          case (b, x: I)                => b += mapper(x)
          case (b, x: java.lang.Number) => b += mapper(x.asInstanceOf[I])
          case (_, x) =>
            fail(s"Retrieved ${x.getClass.getCanonicalName} type from JDBC array, but expected $tag. Re-check your decoder implementation")
        }.result()
      }
    })
  }

  private def rawDecoder[T: ClassTag, Col <: Seq[T]](implicit bf: CanBuildFrom[Nothing, T, Col]): Decoder[Col] =
    arrayDecoder[T, T, Col](identity)
}
