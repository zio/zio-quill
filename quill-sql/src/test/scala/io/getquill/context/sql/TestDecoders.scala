package io.getquill.context.sql

import io.getquill.MappedEncoding

trait TestDecoders {
  implicit val encodingTestTypeDecoder = MappedEncoding[String, EncodingTestType](EncodingTestType)
}
