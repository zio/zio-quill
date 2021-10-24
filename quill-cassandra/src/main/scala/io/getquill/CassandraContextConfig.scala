package io.getquill

import com.datastax.oss.driver.api.core.{ CqlSession, CqlSessionBuilder }
import com.typesafe.config.Config
import io.getquill.context.cassandra.cluster.SessionBuilder

case class CassandraContextConfig(config: Config) {
  def preparedStatementCacheSize: Long =
    if (config.hasPath("preparedStatementCacheSize"))
      config.getLong("preparedStatementCacheSize")
    else
      1000
  def builder: CqlSessionBuilder = SessionBuilder(config.getConfig("session"))
  def session: CqlSession = builder.withKeyspace(keyspace).build()
  def keyspace: String = config.getString("keyspace")
}
