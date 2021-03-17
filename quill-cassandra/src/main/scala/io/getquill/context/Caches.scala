package io.getquill.context

import com.datastax.driver.core.{ BoundStatement, PreparedStatement }
import io.getquill.context.cassandra.PrepareStatementCache
import io.getquill.context.cassandra.util.FutureConversions._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Failure

trait SyncCache { this: CassandraSession =>
  lazy val syncCache = new PrepareStatementCache[PreparedStatement](preparedStatementCacheSize)
  def prepare(cql: String): BoundStatement =
    syncCache(cql)(stmt => session.prepare(stmt)).bind()
}

trait AsyncFutureCache { this: CassandraSession =>
  lazy val asyncCache = new PrepareStatementCache[Future[PreparedStatement]](preparedStatementCacheSize)
  def prepareAsync(cql: String)(implicit executionContext: ExecutionContext): Future[BoundStatement] =
    asyncCache(cql)(stmt => session.prepareAsync(stmt).asScala andThen {
      case Failure(_) => asyncCache.invalidate(stmt)
    }).map(_.bind())
}
