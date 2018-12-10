package io.getquill.context.streaming

import java.sql.Types
import java.util.UUID

trait UUIDStringEncoding[F[_]] { self: StreamingContext[F, _, _] =>
  implicit val uuidEncoder: Encoder[UUID] = encoder(Types.VARCHAR, (index, value, row) => row.setString(index, value.toString))
  implicit val uuidDecoder: Decoder[UUID] = decoder((index, row) => UUID.fromString(row.getString(index)))
}
