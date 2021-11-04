package io.getquill

import com.datastax.oss.driver.api.core.CqlSession
import io.getquill.context.{ AsyncFutureCache, CassandraSession, SyncCache }
import io.getquill.context.cassandra.CassandraSessionContext

abstract class CassandraCqlSessionContext[N <: NamingStrategy](
  val naming:                     N,
  val session:                    CqlSession,
  val preparedStatementCacheSize: Long
)
  extends CassandraSessionContext[N] with CassandraSession
  with SyncCache
  with AsyncFutureCache {
}