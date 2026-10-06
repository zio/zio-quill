package zio.quill

import com.datastax.oss.driver.api.core.{CqlSession, CqlSessionBuilder}
import com.typesafe.config.Config
import zio.quill.context.cassandra.cluster.SessionBuilder

case class CassandraContextConfig(config: Config) {
  def preparedStatementCacheSize: Long =
    if (config.hasPath("preparedStatementCacheSize"))
      config.getLong("preparedStatementCacheSize")
    else
      1000
  def builder: CqlSessionBuilder = SessionBuilder(config.getConfig("session"))
  lazy val session: CqlSession   = builder.withKeyspace(keyspace).build()

  /**
   * the keyspace is from config file. to get actual active keyspace use
   * session.getKeyspace
   * @return
   */
  def keyspace: String = config.getString("keyspace")
}
