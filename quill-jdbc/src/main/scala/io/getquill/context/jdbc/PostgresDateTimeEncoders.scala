package io.getquill.context.jdbc

import java.sql.Types._
import java.time.{ LocalDate, LocalDateTime, LocalTime, OffsetDateTime }

trait PostgresDateTimeEncoders {
  self: JdbcContextBase[_, _] with Encoders =>
  // Taken from https://jdbc.postgresql.org/documentation/head/8-date-time.html

  final private[this] def objectEncoder[T](sqlType: Int): Encoder[T] = encoder(
    sqlType,
    (index, value, row) => row.setObject(index, value)
  )

  override implicit val localDateEncoder: Encoder[LocalDate] = objectEncoder[LocalDate](DATE)
  implicit val localTimeEncoder: Encoder[LocalTime] = objectEncoder[LocalTime](TIME)
  override implicit val localDateTimeEncoder: Encoder[LocalDateTime] = objectEncoder[LocalDateTime](TIMESTAMP)
  implicit val offsetDateTimeEncoder: Encoder[OffsetDateTime] = objectEncoder[OffsetDateTime](TIMESTAMP_WITH_TIMEZONE)
}
