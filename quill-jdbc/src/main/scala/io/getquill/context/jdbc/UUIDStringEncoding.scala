package io.getquill.context.jdbc

import java.sql.Types
import java.util.UUID

trait UUIDStringEncoding {
  this: JdbcContext[_, _] =>
  implicit val uuidEncoder: Encoder[UUID] = encoder(Types.VARCHAR, (index, value, row) => row.setString(index, value.toString))
  implicit val uuidDecoder: Decoder[UUID] = decoder(Types.VARCHAR, (index, row) => UUID.fromString(row.getString(index)))
}
