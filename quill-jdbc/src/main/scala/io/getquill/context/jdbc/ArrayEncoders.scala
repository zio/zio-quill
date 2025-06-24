package io.getquill.context.jdbc

import java.sql.{Timestamp, Date => SqlDate}
import java.sql.Types._
import java.time.LocalDate
import java.util.Date

import io.getquill.context.sql.encoding.ArrayEncoding
import scala.collection.compat._

trait ArrayEncoders extends ArrayEncoding {
  self: JdbcContextTypes[_, _] =>

  implicit def arrayStringEncoder[Col <: collection.Seq[String]]: Encoder[Col] = arrayRawEncoder[String, Col](VARCHAR)
  implicit def arrayBigDecimalEncoder[Col <: collection.Seq[BigDecimal]]: Encoder[Col] =
    arrayEncoder[BigDecimal, Col](parseJdbcType(NUMERIC), _.bigDecimal)
  implicit def arrayBooleanEncoder[Col <: collection.Seq[Boolean]]: Encoder[Col]     = arrayRawEncoder[Boolean, Col](BOOLEAN)
  implicit def arrayByteEncoder[Col <: collection.Seq[Byte]]: Encoder[Col]           = arrayRawEncoder[Byte, Col](TINYINT)
  implicit def arrayShortEncoder[Col <: collection.Seq[Short]]: Encoder[Col]         = arrayRawEncoder[Short, Col](SMALLINT)
  implicit def arrayIntEncoder[Col <: collection.Seq[Int]]: Encoder[Col]             = arrayRawEncoder[Int, Col](INTEGER)
  implicit def arrayLongEncoder[Col <: collection.Seq[Long]]: Encoder[Col]           = arrayRawEncoder[Long, Col](BIGINT)
  implicit def arrayFloatEncoder[Col <: collection.Seq[Float]]: Encoder[Col]         = arrayRawEncoder[Float, Col](FLOAT)
  implicit def arrayDoubleEncoder[Col <: collection.Seq[Double]]: Encoder[Col]       = arrayRawEncoder[Double, Col](DOUBLE)
  implicit def arrayDateEncoder[Col <: collection.Seq[Date]]: Encoder[Col]           = arrayRawEncoder[Date, Col](TIMESTAMP)
  implicit def arrayTimestampEncoder[Col <: collection.Seq[Timestamp]]: Encoder[Col] = arrayRawEncoder[Timestamp, Col](TIMESTAMP)
  implicit def arrayLocalDateEncoder[Col <: collection.Seq[LocalDate]]: Encoder[Col] =
    arrayEncoder[LocalDate, Col](parseJdbcType(DATE), SqlDate.valueOf)

  /**
   * Generic encoder for JDBC arrays.
   *
   * @param jdbcType
   *   JDBC specific type identification, may be various regarding to JDBC
   *   driver
   * @param mapper
   *   jdbc array accepts AnyRef objects hence a mapper is needed. If input type
   *   of an element of collection is not comfortable with jdbcType then use
   *   this mapper to transform to appropriate type before casting to AnyRef
   * @tparam T
   *   element type
   * @tparam Col
   *   seq type
   * @return
   *   JDBC array encoder
   */
  def arrayEncoder[T, Col <: collection.Seq[T]](jdbcType: String, mapper: T => AnyRef): Encoder[Col] =
    encoder[Col](
      ARRAY,
      (idx: Index, seq: Col, row: PrepareRow) => {
        val bf = implicitly[CBF[AnyRef, Array[AnyRef]]]
        row.setArray(
          idx,
          row.getConnection.createArrayOf(
            jdbcType,
            seq.foldLeft(bf.newBuilder)((b, x) => b += mapper(x)).result()
          )
        )
      }
    )

  /**
   * Creates JDBC array encoder for type `T` which is already supported by
   * database as array element.
   *
   * @param jdbcType
   *   JDBC specific type identification, may be various regarding to JDBC
   *   driver
   * @tparam T
   *   element type
   * @tparam Col
   *   seq type
   * @return
   *   JDBC array encoder
   */
  def arrayRawEncoder[T, Col <: collection.Seq[T]](jdbcType: String): Encoder[Col] =
    arrayEncoder[T, Col](jdbcType, _.asInstanceOf[AnyRef])

  /**
   * Transform jdbcType int using `parseJdbcType` and calls overloaded method to
   * create Encoder
   *
   * @param jdbcType
   *   java.sql.Types
   * @see
   *   arrayRawEncoder(jdbcType: String)
   * @see
   *   JdbcContext#parseJdbcType(jdbcType: String)
   */
  def arrayRawEncoder[T, Col <: collection.Seq[T]](jdbcType: Int): Encoder[Col] =
    arrayRawEncoder[T, Col](parseJdbcType(jdbcType))
}
