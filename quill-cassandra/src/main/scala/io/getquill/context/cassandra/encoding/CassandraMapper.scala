package io.getquill.context.cassandra.encoding

import io.getquill.context.UdtValueLookup

/**
 * Developers API.
 *
 * End-users should rely on MappedEncoding since it's more general.
 */
case class CassandraMapper[I, O](f: (I, UdtValueLookup) => O)
object CassandraMapper {
  def simple[I, O](f: I => O): CassandraMapper[I, O] = CassandraMapper[I, O]((iOrig, _) => f(iOrig))
}

