package io.getquill.context.jdbc

import java.sql.{ Timestamp, Types, Date => SqlDate }
import java.time.LocalDate
import java.util.Date

import io.getquill.context.sql.dsl.ArrayEncoding

import scala.collection.generic.CanBuildFrom

trait ArrayEncoders extends ArrayEncoding {
  self: JdbcContext[_, _] =>

  implicit def arrayStringEncoder[Col <: Seq[String]]: Encoder[Col] = rawEncoder[String, Col](Types.VARCHAR)
  implicit def arrayBigDecimalEncoder[Col <: Seq[BigDecimal]]: Encoder[Col] = arrayEncoder[BigDecimal, Col](Types.NUMERIC, _.bigDecimal)
  implicit def arrayBooleanEncoder[Col <: Seq[Boolean]]: Encoder[Col] = rawEncoder[Boolean, Col](Types.BOOLEAN)
  implicit def arrayByteEncoder[Col <: Seq[Byte]]: Encoder[Col] = rawEncoder[Byte, Col](Types.TINYINT)
  implicit def arrayShortEncoder[Col <: Seq[Short]]: Encoder[Col] = rawEncoder[Short, Col](Types.SMALLINT)
  implicit def arrayIntEncoder[Col <: Seq[Int]]: Encoder[Col] = rawEncoder[Int, Col](Types.INTEGER)
  implicit def arrayLongEncoder[Col <: Seq[Long]]: Encoder[Col] = rawEncoder[Long, Col](Types.BIGINT)
  implicit def arrayFloatEncoder[Col <: Seq[Float]]: Encoder[Col] = rawEncoder[Float, Col](Types.FLOAT)
  implicit def arrayDoubleEncoder[Col <: Seq[Double]]: Encoder[Col] = rawEncoder[Double, Col](Types.DOUBLE)
  implicit def arrayDateEncoder[Col <: Seq[Date]]: Encoder[Col] = rawEncoder[Date, Col](Types.TIMESTAMP)
  implicit def arrayTimestampEncoder[Col <: Seq[Timestamp]]: Encoder[Col] = rawEncoder[Timestamp, Col](Types.TIMESTAMP)
  implicit def arrayLocalDateEncoder[Col <: Seq[LocalDate]]: Encoder[Col] = arrayEncoder[LocalDate, Col](Types.DATE, SqlDate.valueOf)

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
  def arrayEncoder[T, Col <: Seq[T]](jdbcType: Int, mapper: T => AnyRef): Encoder[Col] = {
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

  private def rawEncoder[T, Col <: Seq[T]](jdbcType: Int): Encoder[Col] =
    arrayEncoder[T, Col](jdbcType, _.asInstanceOf[AnyRef])
}
