package io.getquill

trait TestEncoders {
  implicit val encodingTestTypeEncoder = MappedEncoding[EncodingTestType, String](_.value)
}
