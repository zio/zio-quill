package io.getquill.context.cassandra.encoding

import com.datastax.oss.driver.api.core.data.UdtValue
import io.getquill.Udt
import io.getquill.context.cassandra.CassandraRowContext

import scala.language.experimental.macros

trait UdtEncoding {
  this: CassandraRowContext[_] =>

  implicit def udtDecoder[T <: Udt]: Decoder[T] = macro UdtEncodingMacro.udtDecoder[T]
  implicit def udtEncoder[T <: Udt]: Encoder[T] = macro UdtEncodingMacro.udtEncoder[T]

  implicit def udtDecodeMapper[T <: Udt]: CassandraMapper[UdtValue, T] = macro UdtEncodingMacro.udtDecodeMapper[T]
  implicit def udtEncodeMapper[T <: Udt]: CassandraMapper[T, UdtValue] = macro UdtEncodingMacro.udtEncodeMapper[T]

}
