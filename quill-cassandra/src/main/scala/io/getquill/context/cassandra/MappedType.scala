package io.getquill.context.cassandra

import com.datastax.driver.core.TypeCodec

case class MappedType[T, Cas](encode: T => Cas, decode: Cas => T, codec: TypeCodec[Cas])