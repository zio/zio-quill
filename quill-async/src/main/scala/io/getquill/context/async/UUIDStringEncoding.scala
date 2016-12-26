package io.getquill.context.async

import java.util.UUID

trait UUIDStringEncoding {
  this: AsyncContext[_, _, _] =>

  implicit val uuidEncoder: Encoder[UUID] = encoder[UUID]((v: UUID) => v.toString, SqlTypes.UUID)

  implicit val uuidDecoder: Decoder[UUID] =
    AsyncDecoder(SqlTypes.UUID)(
      (index: Index, row: ResultRow) => row(index) match {
        case value: String => UUID.fromString(value)
      }
    )
}
