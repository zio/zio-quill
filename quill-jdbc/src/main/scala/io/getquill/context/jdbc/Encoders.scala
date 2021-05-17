package io.getquill.context.jdbc

import java.sql.{ Date, Timestamp, Types }
import java.time.{ LocalDate, LocalDateTime }
import java.util.{ Calendar, TimeZone }
import java.{ sql, util }

trait Encoders {
  this: JdbcRunContext[_, _] =>

  type Encoder[T] = JdbcEncoder[T]

  protected val dateTimeZone = TimeZone.getDefault

  case class JdbcEncoder[T](sqlType: Int, encoder: BaseEncoder[T]) extends BaseEncoder[T] {
    override def apply(index: Index, value: T, row: PrepareRow) =
      encoder(index + 1, value, row)
  }

  def encoder[T](sqlType: Int, f: (Index, T, PrepareRow) => Unit): Encoder[T] =
    JdbcEncoder(sqlType, (index: Index, value: T, row: PrepareRow) => {
      f(index, value, row)
      row
    })

  def encoder[T](sqlType: Int, f: PrepareRow => (Index, T) => Unit): Encoder[T] =
    encoder(sqlType, (index: Index, value: T, row: PrepareRow) => f(row)(index, value))

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    JdbcEncoder(e.sqlType, mappedBaseEncoder(mapped, e.encoder))

  private[this] val nullEncoder: Encoder[Int] = encoder(Types.INTEGER, _.setNull)

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    JdbcEncoder(
      d.sqlType,
      (index, value, row) =>
        value match {
          case Some(v) => d.encoder(index, v, row)
          case None    => nullEncoder.encoder(index, d.sqlType, row)
        }
    )

  implicit val stringEncoder: Encoder[String] = encoder(Types.VARCHAR, _.setString)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    encoder(Types.NUMERIC, (index, value, row) => row.setBigDecimal(index, value.bigDecimal))
  implicit val byteEncoder: Encoder[Byte] = encoder(Types.TINYINT, _.setByte)
  implicit val shortEncoder: Encoder[Short] = encoder(Types.SMALLINT, _.setShort)
  implicit val intEncoder: Encoder[Int] = encoder(Types.INTEGER, _.setInt)
  implicit val longEncoder: Encoder[Long] = encoder(Types.BIGINT, _.setLong)
  implicit val floatEncoder: Encoder[Float] = encoder(Types.FLOAT, _.setFloat)
  implicit val doubleEncoder: Encoder[Double] = encoder(Types.DOUBLE, _.setDouble)
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder(Types.VARBINARY, _.setBytes)
  implicit val dateEncoder: Encoder[util.Date] =
    encoder(Types.TIMESTAMP, (index, value, row) =>
      row.setTimestamp(index, new sql.Timestamp(value.getTime), Calendar.getInstance(dateTimeZone)))
  implicit val localDateEncoder: Encoder[LocalDate] =
    encoder(Types.DATE, (index, value, row) =>
      row.setDate(index, Date.valueOf(value), Calendar.getInstance(dateTimeZone)))
  implicit val localDateTimeEncoder: Encoder[LocalDateTime] =
    encoder(Types.TIMESTAMP, (index, value, row) =>
      row.setTimestamp(index, Timestamp.valueOf(value), Calendar.getInstance(dateTimeZone)))
}