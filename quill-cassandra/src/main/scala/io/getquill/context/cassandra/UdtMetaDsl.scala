package io.getquill.context.cassandra

import scala.language.experimental.macros
import io.getquill.Udt

trait UdtMetaDsl {
  this: CassandraContext[_] =>

  /**
   * Creates udt meta to override udt name / keyspace and rename columns
   *
   * @param path - either `udt_name` or `keyspace.udt_name`
   * @param columns - columns to rename
   * @return udt meta
   */
  def udtMeta[T <: Udt](path: String, columns: (T => (Any, String))*): UdtMeta[T] = macro UdtMetaDslMacro.udtMeta[T]

  trait UdtMeta[T <: Udt] {
    def keyspace: Option[String]
    def name: String
    def alias(col: String): Option[String]
  }
}
