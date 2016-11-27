package io.getquill

trait TestDecoders {
  implicit val encodingTestTypeDecoder = MappedEncoding[String, EncodingTestType](EncodingTestType)
}
