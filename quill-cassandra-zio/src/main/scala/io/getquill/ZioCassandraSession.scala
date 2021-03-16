package io.getquill

import com.datastax.driver.core.{ BoundStatement, Cluster, PreparedStatement }
import io.getquill.CassandraZioContext.CIO
import io.getquill.context.{ CassandraSession, SyncCache }
import io.getquill.context.cassandra.PrepareStatementCache
import zio.ZIO
import io.getquill.util.ZioConversions._

trait ZioAsyncCache { self: CassandraSession =>
  lazy val asyncCache = new PrepareStatementCache[CIO[PreparedStatement]](preparedStatementCacheSize)
  def prepareAsync(cql: String): CIO[BoundStatement] =
    asyncCache(cql)(stmt => session.prepareAsync(stmt).asCio.tapError {
      _ => ZIO.effect(asyncCache.invalidate(stmt))
    }).map(_.bind())
}

case class ZioCassandraSession(
  override val cluster:                    Cluster,
  override val keyspace:                   String,
  override val preparedStatementCacheSize: Long
) extends CassandraSession with SyncCache with ZioAsyncCache with AutoCloseable
