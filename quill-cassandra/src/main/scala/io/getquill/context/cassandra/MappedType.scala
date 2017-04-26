package io.getquill.context.cassandra

case class MappedType[T, Cas](encode: T => Cas, decode: Cas => T)