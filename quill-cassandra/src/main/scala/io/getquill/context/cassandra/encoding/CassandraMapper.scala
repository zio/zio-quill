package io.getquill.context.cassandra.encoding

/**
 * Developers API.
 *
 * End-users should rely on MappedEncoding since it's more general.
 */
case class CassandraMapper[I, O](f: I => O)
