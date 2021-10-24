package io.getquill

import com.datastax.oss.driver.api.core.CqlSession
import io.getquill.context.{ AsyncFutureCache, CassandraSession, SyncCache }
import io.getquill.context.cassandra.CassandraSessionContext

abstract class CassandraCqlSessionContext[N <: NamingStrategy](
  val naming:                     N,
  val session:                    CqlSession,
  val keyspace:                   String, //TODO - we do not really need the keyspace field here - it can be provided via config and can be changed by execute('use  some_keyspace')
  val preparedStatementCacheSize: Long
)
  extends CassandraSessionContext[N] with CassandraSession
  with SyncCache
  with AsyncFutureCache {
  //def keyspace = session.getKeyspace.toString
}