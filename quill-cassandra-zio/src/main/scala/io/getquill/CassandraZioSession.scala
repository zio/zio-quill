package io.getquill

import com.datastax.driver.core.Cluster
import com.typesafe.config.Config
import io.getquill.context.{ CassandraSession, SyncCache }
import io.getquill.util.LoadConfig
import zio._
import zio.blocking.Blocking

case class CassandraZioSession(
  override val cluster:                    Cluster,
  override val keyspace:                   String,
  override val preparedStatementCacheSize: Long
) extends CassandraSession with SyncCache with AsyncZioCache with AutoCloseable

object CassandraZioSession {
  def fromContextConfig(config: CassandraContextConfig) = {
    val managed =
      for {
        env <- ZManaged.environment[Blocking]
        block = env.get[Blocking.Service]
        // Evaluate the configuration inside of 'effect' and then create the session from it
        session <- ZManaged.fromAutoCloseable(
          ZIO.effect(CassandraZioSession(config.cluster, config.keyspace, config.preparedStatementCacheSize))
        )
      } yield Has(session) ++ Has(block)
    ZLayer.fromManagedMany(managed)
  }

  def fromConfig(config: Config) = fromContextConfig(CassandraContextConfig(config))
  // Call the by-name constructor for the construction to fail inside of the effect if it fails
  def fromPrefix(configPrefix: String) = fromContextConfig(CassandraContextConfig(LoadConfig(configPrefix)))
}