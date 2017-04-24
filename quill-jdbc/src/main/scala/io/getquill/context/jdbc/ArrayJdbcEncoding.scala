package io.getquill.context.jdbc

import java.sql.{ JDBCType, Timestamp, Types }
import java.time.LocalDate
import java.util.Date
import java.sql.{ Date => SqlDate }
import java.math.{ BigDecimal => JBigDecimal }

import io.getquill.context.sql.dsl.ArrayEncoding
import io.getquill.util.Messages.fail

import scala.collection.generic.CanBuildFrom
import scala.reflect.ClassTag

/**
 * Base trait for encoding JDBC arrays.
 * Mix this trait into instances of `JdbcContext` in order to provide array support.
 * Note that not all JDBC drivers/databases support arrays.
 * We say `array` only in scope of JDBC. In Quill we represent them as instances of Traversable
 */
trait ArrayJdbcEncoding extends ArrayEncoding {
  self: JdbcContext[_, _] =>

  implicit def arrayStringEncoder[Col <: Traversable[String]]: Encoder[Col] = rawEncoder[String, Col](Types.VARCHAR)
  implicit def arrayBigDecimalEncoder[Col <: Traversable[BigDecimal]]: Encoder[Col] = arrayEncoder[BigDecimal, Col](Types.NUMERIC, _.bigDecimal)
  implicit def arrayBooleanEncoder[Col <: Traversable[Boolean]]: Encoder[Col] = rawEncoder[Boolean, Col](Types.BOOLEAN)
  implicit def arrayByteEncoder[Col <: Traversable[Byte]]: Encoder[Col] = rawEncoder[Byte, Col](Types.TINYINT)
  implicit def arrayShortEncoder[Col <: Traversable[Short]]: Encoder[Col] = rawEncoder[Short, Col](Types.SMALLINT)
  implicit def arrayIntEncoder[Col <: Traversable[Int]]: Encoder[Col] = rawEncoder[Int, Col](Types.INTEGER)
  implicit def arrayLongEncoder[Col <: Traversable[Long]]: Encoder[Col] = rawEncoder[Long, Col](Types.BIGINT)
  implicit def arrayFloatEncoder[Col <: Traversable[Float]]: Encoder[Col] = rawEncoder[Float, Col](Types.FLOAT)
  implicit def arrayDoubleEncoder[Col <: Traversable[Double]]: Encoder[Col] = rawEncoder[Double, Col](Types.DOUBLE)
  implicit def arrayDateEncoder[Col <: Traversable[Date]]: Encoder[Col] = rawEncoder[Date, Col](Types.TIMESTAMP)
  implicit def arrayTimestampEncoder[Col <: Traversable[Timestamp]]: Encoder[Col] = rawEncoder[Timestamp, Col](Types.TIMESTAMP)
  implicit def arrayLocalDateEncoder[Col <: Traversable[LocalDate]]: Encoder[Col] = arrayEncoder[LocalDate, Col](Types.DATE, SqlDate.valueOf)

  implicit def arrayStringDecoder[Col <: Traversable[String]](implicit bf: CanBuildFrom[Nothing, String, Col]): Decoder[Col] = rawDecoder[String, Col]
  implicit def arrayBigDecimalDecoder[Col <: Traversable[BigDecimal]](implicit bf: CBF[BigDecimal, Col]): Decoder[Col] = arrayDecoder[JBigDecimal, BigDecimal, Col](BigDecimal.apply)
  implicit def arrayBooleanDecoder[Col <: Traversable[Boolean]](implicit bf: CBF[Boolean, Col]): Decoder[Col] = rawDecoder[Boolean, Col]
  implicit def arrayByteDecoder[Col <: Traversable[Byte]](implicit bf: CBF[Byte, Col]): Decoder[Col] = rawDecoder[Byte, Col]
  implicit def arrayShortDecoder[Col <: Traversable[Short]](implicit bf: CBF[Short, Col]): Decoder[Col] = rawDecoder[Short, Col]
  implicit def arrayIntDecoder[Col <: Traversable[Int]](implicit bf: CBF[Int, Col]): Decoder[Col] = rawDecoder[Int, Col]
  implicit def arrayLongDecoder[Col <: Traversable[Long]](implicit bf: CBF[Long, Col]): Decoder[Col] = rawDecoder[Long, Col]
  implicit def arrayFloatDecoder[Col <: Traversable[Float]](implicit bf: CBF[Float, Col]): Decoder[Col] = rawDecoder[Float, Col]
  implicit def arrayDoubleDecoder[Col <: Traversable[Double]](implicit bf: CBF[Double, Col]): Decoder[Col] = rawDecoder[Double, Col]
  implicit def arrayDateDecoder[Col <: Traversable[Date]](implicit bf: CBF[Date, Col]): Decoder[Col] = rawDecoder[Date, Col]
  implicit def arrayTimestampDecoder[Col <: Traversable[Timestamp]](implicit bf: CBF[Timestamp, Col]): Decoder[Col] = rawDecoder[Timestamp, Col]
  implicit def arrayLocalDateDecoder[Col <: Traversable[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col] = arrayDecoder[SqlDate, LocalDate, Col](_.toLocalDate)

  /**
   * Parses instances of java.sql.Types to string form so it can be used in creation of sql arrays.
   * Some databases does not support each of generic types, as with Postgres and `TINYINT`.
   * Hence it's welcome to override this method and provide alternatives to non-existent types.
   *
   * @param intType one of java.sql.Types
   * @return JDBC type in string form
   */
  def parseJdbcType(intType: Int): String = JDBCType.valueOf(intType).getName

  /**
   * Generic encoder for JDBC arrays.
   *
   * @param jdbcType one of java.sql.Types, used to create JDBC array base
   * @param mapper jdbc array accepts AnyRef objects hence a mapper is needed.
   *               If input type of an element of collection is not comfortable with jdbcType
   *               then use this mapper to transform to appropriate type before casting to AnyRef
   * @tparam T type of an element of collection to encode
   * @tparam Col type of collection to encode
   * @return JDBC array encoder
   */
  def arrayEncoder[T, Col <: Traversable[T]](jdbcType: Int, mapper: T => AnyRef): Encoder[Col] = {
    encoder[Col](Types.ARRAY, (idx: Index, seq: Col, row: PrepareRow) => {
      val bf = implicitly[CanBuildFrom[Nothing, AnyRef, Array[AnyRef]]]
      row.setArray(
        idx,
        withConnection(_.createArrayOf(
          parseJdbcType(jdbcType),
          seq.foldLeft(bf())((b, x) => b += mapper(x)).result()
        ))
      )
    })
  }

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
  def arrayDecoder[I, O, Col <: Traversable[O]](mapper: I => O)(implicit bf: CanBuildFrom[Nothing, O, Col], tag: ClassTag[I]): Decoder[Col] = {
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

  private def rawEncoder[T, Col <: Traversable[T]](jdbcType: Int): Encoder[Col] =
    arrayEncoder[T, Col](jdbcType, _.asInstanceOf[AnyRef])

  private def rawDecoder[T: ClassTag, Col <: Traversable[T]](implicit bf: CanBuildFrom[Nothing, T, Col]): Decoder[Col] =
    arrayDecoder[T, T, Col](identity)
}
