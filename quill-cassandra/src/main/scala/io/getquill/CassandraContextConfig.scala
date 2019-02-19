package io.getquill

import com.datastax.driver.core.Cluster
import com.typesafe.config.Config
import io.getquill.context.cassandra.cluster.ClusterBuilder

case class CassandraContextConfig(config: Config) {
  def preparedStatementCacheSize: Long =
    if (config.hasPath("preparedStatementCacheSize"))
      config.getLong("preparedStatementCacheSize")
    else
      1000
  def builder = ClusterBuilder(config.getConfig("session"))
  def cluster: Cluster = builder.build
  def keyspace: String = config.getString("keyspace")
}
