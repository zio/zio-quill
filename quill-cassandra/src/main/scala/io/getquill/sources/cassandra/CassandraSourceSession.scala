package io.getquill.sources.cassandra

import scala.util.Try
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Row
import com.datastax.driver.core.Session
import com.typesafe.scalalogging.StrictLogging
import io.getquill.naming.NamingStrategy
import io.getquill.sources.cassandra.cluster.ClusterBuilder
import io.getquill.sources.cassandra.encoding.Decoders
import io.getquill.sources.cassandra.encoding.Encoders
import io.getquill.CassandraSourceConfig

abstract class CassandraSourceSession[N <: NamingStrategy](config: CassandraSourceConfig[N, _])
    extends CassandraSource[N, Row, BoundStatement]
    with StrictLogging
    with Encoders
    with Decoders {

  private val cluster = config.cluster
  protected val session = cluster.connect(config.keyspace)

  protected val preparedStatementCache =
    new PrepareStatementCache(config.preparedStatementCacheSize)

  protected def prepare(cql: String): BoundStatement =
    preparedStatementCache(cql)(session.prepare)

  protected def prepare(cql: String, bind: BoundStatement => BoundStatement): BoundStatement =
    bind(prepare(cql))

  def close() = {
    session.close
    cluster.close
  }

  def probe(cql: String) =
    Try {
      prepare(cql)
      ()
    }
}
