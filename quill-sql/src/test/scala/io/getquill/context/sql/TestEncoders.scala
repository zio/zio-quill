package io.getquill.context.sql

import io.getquill.context.Context

trait TestEncoders {
  this: SqlContext[_, _, _, _] with Context[_, _] =>

  implicit val encodingTestTypeEncoder = mappedEncoding[EncodingTestType, String](_.value)
}
