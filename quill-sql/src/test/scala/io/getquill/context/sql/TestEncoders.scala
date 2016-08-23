package io.getquill.context.sql

trait TestEncoders {
  this: SqlContext[_, _] =>

  implicit val encodingTestTypeEncoder = mappedEncoding[EncodingTestType, String](_.value)
}
