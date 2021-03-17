package io.getquill

import com.datastax.driver.core.Cluster
import io.getquill.context.{ CassandraSession, AsyncFutureCache, SyncCache }
import io.getquill.context.cassandra.CassandraSessionContext

abstract class CassandraClusterSessionContext[N <: NamingStrategy](
  val naming:                     N,
  val cluster:                    Cluster,
  val keyspace:                   String,
  val preparedStatementCacheSize: Long
)
  extends CassandraSessionContext[N] with CassandraSession
  with SyncCache
  with AsyncFutureCache
