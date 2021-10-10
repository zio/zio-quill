package io.getquill

import com.datastax.driver.core.Cluster
import com.typesafe.config.Config
import io.getquill.context.{ AsyncFutureCache, CassandraSession, SyncCache }
import io.getquill.util.LoadConfig
import zio.{ Has, ZIO, ZLayer, ZManaged }

case class CassandraZioSession(
  override val cluster:                    Cluster,
  override val keyspace:                   String,
  override val preparedStatementCacheSize: Long
) extends CassandraSession with SyncCache with AsyncFutureCache with AutoCloseable

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