package io.getquill.context.streaming
import java.sql.Types
import java.util.UUID

trait UUIDObjectEncoding[F[_]] { self: StreamingContext[F, _, _] =>
  implicit val uuidEncoder: Encoder[UUID] = encoder(Types.OTHER, (index, value, row) => row.setObject(index, value, Types.OTHER))
  implicit val uuidDecoder: Decoder[UUID] = decoder((index, row) => UUID.fromString(row.getObject(index).toString))
}
