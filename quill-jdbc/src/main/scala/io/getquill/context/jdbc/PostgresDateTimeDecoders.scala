package io.getquill.context.jdbc

import java.time.{ LocalDate, LocalDateTime, LocalTime, OffsetDateTime }

trait PostgresDateTimeDecoders {
  self: JdbcContextBase[_, _] with Decoders =>
  // Taken from https://jdbc.postgresql.org/documentation/head/8-date-time.html

  override implicit val localDateDecoder: Decoder[LocalDate] =
    decoder(
      (index, row) =>
        row.getObject(index, classOf[LocalDate])
    )

  implicit val localTimeDecoder: Decoder[LocalTime] =
    decoder(
      (index, row) =>
        row.getObject(index, classOf[LocalTime])
    )

  override implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    decoder(
      (index, row) =>
        row.getObject(index, classOf[LocalDateTime])
    )

  implicit val offsetDateTimeDecoder: Decoder[OffsetDateTime] =
    decoder(
      (index, row) =>
        row.getObject(index, classOf[OffsetDateTime])
    )

}
