package io.getquill.context.sql

import io.getquill.MappedEncoding

trait TestEncoders {
  implicit val encodingTestTypeEncoder: MappedEncoding[EncodingTestType,String] = MappedEncoding[EncodingTestType, String](_.value)
  implicit val nameEncoder: MappedEncoding[Number,String]             = MappedEncoding[Number, String](_.value)
}
