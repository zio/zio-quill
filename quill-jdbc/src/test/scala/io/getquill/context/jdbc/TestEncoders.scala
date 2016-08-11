package io.getquill.context.jdbc

import java.sql.Types

import io.getquill.JdbcContext
import io.getquill.context.Context
import io.getquill.context.sql.EncodingTestType

trait TestEncoders extends JdbcEncoders with io.getquill.context.sql.TestEncoders {
  this: JdbcContext[_, _] with Context[_, _] =>

  implicit val jdbcEncodingTestTypeEncoder = JdbcEncoder[EncodingTestType](Types.VARCHAR)(mappedEncoder(encodingTestTypeEncoder, stringEncoder))
}
