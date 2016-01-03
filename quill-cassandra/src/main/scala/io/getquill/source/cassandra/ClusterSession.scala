package io.getquill.source.cassandra

import com.datastax.driver.core.Cluster
import com.typesafe.config.Config
import com.datastax.driver.core.QueryOptions
import com.datastax.driver.core.ConsistencyLevel

object ClusterSession {

  def apply(config: Config) = {
    var builder = Cluster.builder

    builder = builder.addContactPoints(config.getString("contactPoints"))

    if (config.hasPath("port"))
      builder = builder.withPort(config.getInt("port"))

    if (config.hasPath("user") && config.hasPath("password"))
      builder = builder.withCredentials(config.getString("user"), config.getString("password"))

    if (config.hasPath("ssl") && config.getBoolean("ssl"))
      builder = builder.withSSL

    builder.withQueryOptions((new QueryOptions).setConsistencyLevel(ConsistencyLevel.ALL)).build.connect(config.getString("keyspace"))
  }
}
