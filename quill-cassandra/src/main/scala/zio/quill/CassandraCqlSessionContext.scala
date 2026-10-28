package zio.quill

import com.datastax.oss.driver.api.core.CqlSession
import zio.quill.context.{AsyncFutureCache, CassandraSession, SyncCache}
import zio.quill.context.cassandra.CassandraSessionContext

abstract class CassandraCqlSessionContext[+N <: NamingStrategy](
  val naming: N,
  val session: CqlSession,
  val preparedStatementCacheSize: Long
) extends CassandraSessionContext[N]
    with CassandraSession
    with SyncCache
    with AsyncFutureCache {}
