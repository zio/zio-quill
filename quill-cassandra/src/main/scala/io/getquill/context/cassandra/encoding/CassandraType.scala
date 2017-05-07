package io.getquill.context.cassandra.encoding

/**
 * Marker which signals that type `T` is already supported by Cassandra
 */
trait CassandraType[T]
object CassandraType {
  def of[T]: CassandraType[T] = new CassandraType[T] {}
}