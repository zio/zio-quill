package io.getquill.context.async

import java.util.UUID

trait UUIDObjectEncoding {
  this: AsyncContext[_, _, _] =>

  implicit val uuidEncoder: Encoder[UUID] = encoder[UUID](SqlTypes.UUID)

  implicit val uuidDecoder: Decoder[UUID] =
    AsyncDecoder(SqlTypes.UUID)(
      (index: Index, row: ResultRow) => row(index) match {
        case value: UUID => value
      }
    )
}
