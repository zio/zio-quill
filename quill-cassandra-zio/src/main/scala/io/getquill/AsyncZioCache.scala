package io.getquill

import com.datastax.driver.core.{ BoundStatement, PreparedStatement }
import io.getquill.CassandraZioContext.CIO
import io.getquill.context.CassandraSession
import io.getquill.util.ZioConversions._
import zio.Runtime
import zio.cache.{ Cache, Lookup }
import zio.duration.Duration

trait AsyncZioCache { self: CassandraSession =>
  lazy val asyncCache: Cache[String, Throwable, PreparedStatement] =
    Runtime.default.unsafeRun(Cache.make(preparedStatementCacheSize.toInt, Duration.Infinity, Lookup(stmt => session.prepareAsync(stmt).asZio)))
  def prepareAsync(cql: String): CIO[BoundStatement] =
    asyncCache
      .get(cql)
      .tapError(_ => asyncCache.invalidate(cql))
      .map(_.bind())
}