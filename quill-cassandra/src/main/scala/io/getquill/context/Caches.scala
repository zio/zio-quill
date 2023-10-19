package io.getquill.context

import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}
import io.getquill.context.cassandra.PrepareStatementCache
import io.getquill.context.cassandra.util.FutureConversions._

import java.util.concurrent.CompletionStage
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure
import scala.compat.java8.FutureConverters._

trait SyncCache { this: CassandraSession =>
  lazy val syncCache = new PrepareStatementCache[PreparedStatement](preparedStatementCacheSize)
  def prepare(cql: String): BoundStatement =
    syncCache(cql)(stmt => session.prepare(stmt)).bind()
}

trait AsyncFutureCache { this: CassandraSession =>
  lazy val asyncCache = new PrepareStatementCache[CompletionStage[PreparedStatement]](preparedStatementCacheSize)

  def prepareAsync(cql: String)(implicit executionContext: ExecutionContext): Future[BoundStatement] = {
    val output = asyncCache(cql) { stmt =>
      session.prepareAsync(stmt)
    }.toScala

    output.onComplete {
      case Failure(_) => asyncCache.invalidate(cql)
      case _          => ()
    }
    output.map(_.bind())

  }
}
