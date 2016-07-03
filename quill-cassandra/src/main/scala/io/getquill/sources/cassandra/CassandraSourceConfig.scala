package io.getquill.sources.cassandra

import com.datastax.driver.core.Cluster
import io.getquill.sources.cassandra.cluster.ClusterBuilder
import com.typesafe.config.Config

case class CassandraSourceConfig(config: Config) {
  def preparedStatementCacheSize =
    if (config.hasPath("preparedStatementCacheSize"))
      config.getLong("preparedStatementCacheSize")
    else
      1000
  def builder = ClusterBuilder(config.getConfig("session"))
  def cluster: Cluster = builder.build
  def keyspace: String = config.getString("keyspace")
}
