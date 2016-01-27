package io.getquill

import io.getquill.sources.SourceConfig
import com.datastax.driver.core.Cluster
import io.getquill.sources.cassandra.cluster.ClusterBuilder
import io.getquill.naming.NamingStrategy
import io.getquill.sources.cassandra.CassandraAsyncSource
import io.getquill.sources.cassandra.CassandraStreamSource
import io.getquill.sources.cassandra.CassandraSyncSource

abstract class CassandraSourceConfig[N <: NamingStrategy, T](val name: String) extends SourceConfig[T] {
  def preparedStatementCacheSize =
    if (config.hasPath("preparedStatementCacheSize"))
      config.getLong("preparedStatementCacheSize")
    else
      1000
  def builder = ClusterBuilder(config.getConfig("session"))
  def cluster: Cluster = builder.build
  def keyspace: String = config.getString("keyspace")
}

class CassandraAsyncSourceConfig[N <: NamingStrategy](name: String)
  extends CassandraSourceConfig[N, CassandraAsyncSource[N]](name)

class CassandraStreamSourceConfig[N <: NamingStrategy](name: String)
  extends CassandraSourceConfig[N, CassandraStreamSource[N]](name)

class CassandraSyncSourceConfig[N <: NamingStrategy](name: String)
  extends CassandraSourceConfig[N, CassandraSyncSource[N]](name)
