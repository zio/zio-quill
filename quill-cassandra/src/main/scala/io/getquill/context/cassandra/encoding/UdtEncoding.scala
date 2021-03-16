package io.getquill.context.cassandra.encoding

import com.datastax.driver.core.UDTValue
import io.getquill.Udt
import io.getquill.context.cassandra.CassandraBaseContext

import scala.language.experimental.macros

trait UdtEncoding {
  this: CassandraBaseContext[_] =>

  implicit def udtDecoder[T <: Udt]: Decoder[T] = macro UdtEncodingMacro.udtDecoder[T]
  implicit def udtEncoder[T <: Udt]: Encoder[T] = macro UdtEncodingMacro.udtEncoder[T]

  implicit def udtDecodeMapper[T <: Udt]: CassandraMapper[UDTValue, T] = macro UdtEncodingMacro.udtDecodeMapper[T]
  implicit def udtEncodeMapper[T <: Udt]: CassandraMapper[T, UDTValue] = macro UdtEncodingMacro.udtEncodeMapper[T]

}
