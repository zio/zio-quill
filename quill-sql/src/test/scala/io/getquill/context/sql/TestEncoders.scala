package io.getquill.context.sql

trait TestEncoders {
  this: SqlContext[_, _] =>

  implicit val encodingTestTypeEncoder = MappedEncoding[EncodingTestType, String](_.value)
}
