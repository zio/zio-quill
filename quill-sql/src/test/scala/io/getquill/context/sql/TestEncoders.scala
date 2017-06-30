package io.getquill.context.sql

import io.getquill.MappedEncoding

trait TestEncoders {
  implicit val encodingTestTypeEncoder = MappedEncoding[EncodingTestType, String](_.value)
}
