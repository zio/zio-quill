package io.getquill

import com.datastax.driver.core.{ BoundStatement, Cluster, PreparedStatement }
import com.typesafe.config.Config
import io.getquill.CassandraZioContext.CIO
import io.getquill.context.{ CassandraSession, SyncCache }
import io.getquill.context.cassandra.PrepareStatementCache
import io.getquill.util.LoadConfig
import zio.{ Has, ZIO, ZLayer, ZManaged }
import io.getquill.util.ZioConversions._

trait AsyncZioCache { self: CassandraSession =>
  lazy val asyncCache = new PrepareStatementCache[CIO[PreparedStatement]](preparedStatementCacheSize)
  def prepareAsync(cql: String): CIO[BoundStatement] =
    asyncCache(cql)(stmt => session.prepareAsync(stmt).asZio.tapError {
      _ => ZIO.effect(asyncCache.invalidate(stmt))
    }).map(_.bind())
}

case class CassandraZioSession(
  override val cluster:                    Cluster,
  override val keyspace:                   String,
  override val preparedStatementCacheSize: Long
) extends CassandraSession with SyncCache with AsyncZioCache with AutoCloseable

object CassandraZioSession {
  val live: ZLayer[Has[CassandraContextConfig], Throwable, Has[CassandraZioSession]] =
    (for {
      config <- ZManaged.service[CassandraContextConfig]
      // Evaluate the configuration inside of 'effect' and then create the session from it
      session <- ZManaged.fromAutoCloseable(
        ZIO.effect(CassandraZioSession(config.cluster, config.keyspace, config.preparedStatementCacheSize))
      )
    } yield session).toLayer

  def fromContextConfig(config: CassandraContextConfig): ZLayer[Any, Throwable, Has[CassandraZioSession]] =
    ZLayer.succeed(config) >>> live

  def fromConfig(config: Config) = fromContextConfig(CassandraContextConfig(config))
  // Call the by-name constructor for the construction to fail inside of the effect if it fails
  def fromPrefix(configPrefix: String) = fromContextConfig(CassandraContextConfig(LoadConfig(configPrefix)))
}