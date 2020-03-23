package io.getquill.context.jasync

import java.util.UUID

trait UUIDObjectEncoding {
  this: JAsyncContext[_, _, _] =>

  implicit val uuidEncoder: Encoder[UUID] = encoder[UUID](SqlTypes.UUID)

  implicit val uuidDecoder: Decoder[UUID] =
    AsyncDecoder(SqlTypes.UUID)(
      (index: Index, row: ResultRow) => row.get(index) match {
        case value: UUID => value
      }
    )
}
