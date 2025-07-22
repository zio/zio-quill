package io.getquill.context.jdbc

import java.sql.Timestamp
import java.time.LocalDate
import java.util.Date
import java.sql.{Date => SqlDate}
import java.math.{BigDecimal => JBigDecimal}

import io.getquill.context.sql.encoding.ArrayEncoding
import io.getquill.util.Messages.fail

import scala.collection.compat._
import scala.reflect.ClassTag

trait ArrayDecoders extends ArrayEncoding {
  self: JdbcContextTypes[_, _] =>

  implicit def arrayStringDecoder[Col <: collection.Seq[String]](implicit bf: CBF[String, Col]): Decoder[Col] =
    arrayRawDecoder[String, Col]
  implicit def arrayBigDecimalDecoder[Col <: collection.Seq[BigDecimal]](implicit bf: CBF[BigDecimal, Col]): Decoder[Col] =
    arrayDecoder[JBigDecimal, BigDecimal, Col](BigDecimal.apply)
  implicit def arrayBooleanDecoder[Col <: collection.Seq[Boolean]](implicit bf: CBF[Boolean, Col]): Decoder[Col] =
    arrayRawDecoder[Boolean, Col]
  implicit def arrayByteDecoder[Col <: collection.Seq[Byte]](implicit bf: CBF[Byte, Col]): Decoder[Col] =
    arrayRawDecoder[Byte, Col]
  implicit def arrayShortDecoder[Col <: collection.Seq[Short]](implicit bf: CBF[Short, Col]): Decoder[Col] =
    arrayRawDecoder[Short, Col]
  implicit def arrayIntDecoder[Col <: collection.Seq[Int]](implicit bf: CBF[Int, Col]): Decoder[Col] = arrayRawDecoder[Int, Col]
  implicit def arrayLongDecoder[Col <: collection.Seq[Long]](implicit bf: CBF[Long, Col]): Decoder[Col] =
    arrayRawDecoder[Long, Col]
  implicit def arrayFloatDecoder[Col <: collection.Seq[Float]](implicit bf: CBF[Float, Col]): Decoder[Col] =
    arrayRawDecoder[Float, Col]
  implicit def arrayDoubleDecoder[Col <: collection.Seq[Double]](implicit bf: CBF[Double, Col]): Decoder[Col] =
    arrayRawDecoder[Double, Col]
  implicit def arrayDateDecoder[Col <: collection.Seq[Date]](implicit bf: CBF[Date, Col]): Decoder[Col] =
    arrayRawDecoder[Date, Col]
  implicit def arrayTimestampDecoder[Col <: collection.Seq[Timestamp]](implicit bf: CBF[Timestamp, Col]): Decoder[Col] =
    arrayRawDecoder[Timestamp, Col]
  implicit def arrayLocalDateDecoder[Col <: collection.Seq[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col] =
    arrayDecoder[SqlDate, LocalDate, Col](_.toLocalDate)

  /**
   * Generic encoder for JDBC arrays.
   *
   * @param mapper
   *   retrieved raw types for JDBC array may be mapped via this mapper to
   *   satisfy encoder type
   * @param bf
   *   builder factory is needed to create instances of decoder's collection
   * @tparam I
   *   raw type retrieved form JDBC array
   * @tparam O
   *   mapped type fulfilled in decoder's collection
   * @tparam Col
   *   seq type
   * @return
   *   JDBC array decoder
   */
  def arrayDecoder[I, O, Col <: collection.Seq[O]](mapper: I => O)(implicit bf: CBF[O, Col], tag: ClassTag[I]): Decoder[Col] =
    decoder[Col] { (idx: Index, row: ResultRow, session: Session) =>
      val arr  = row.getArray(idx)
      val bldr = bf.newBuilder
      if (arr == null) bldr.result()
      else {
        val a = arr.getArray
          .asInstanceOf[Array[AnyRef]]
        bldr.sizeHint(a)
        a.foldLeft(bldr) {
          case (b, x: I)                => b += mapper(x)
          case (b, x: java.lang.Number) => b += mapper(x.asInstanceOf[I])
          case (_, x) =>
            fail(
              s"Retrieved ${x.getClass.getCanonicalName} type from JDBC array, but expected $tag. Re-check your decoder implementation"
            )
        }.result()
      }
    }

  /**
   * Creates JDBC array decoder for type `T` which is already supported by
   * database as array element.
   *
   * @param bf
   *   builder factory is needed to create instances of decoder's collection
   * @tparam T
   *   element type
   * @tparam Col
   *   seq type
   * @return
   *   JDBC array decoder
   */
  def arrayRawDecoder[T: ClassTag, Col <: collection.Seq[T]](implicit bf: CBF[T, Col]): Decoder[Col] =
    arrayDecoder[T, T, Col](identity)
}
