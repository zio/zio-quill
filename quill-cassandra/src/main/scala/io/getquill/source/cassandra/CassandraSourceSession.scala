package io.getquill.source.cassandra

import scala.util.Try

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Row
import com.datastax.driver.core.Session
import com.typesafe.scalalogging.StrictLogging

import io.getquill.naming.NamingStrategy
import io.getquill.source.cassandra.cluster.ClusterBuilder
import io.getquill.source.cassandra.encoding.Decoders
import io.getquill.source.cassandra.encoding.Encoders

trait CassandraSourceSession[N <: NamingStrategy]
  extends CassandraSource[N, Row, BoundStatement]
  with StrictLogging
  with Encoders
  with Decoders {

  protected val cluster: Cluster = ClusterBuilder(config.getConfig("session")).build
  protected val session: Session = cluster.connect(config.getString("keyspace"))

  protected val preparedStatementCache =
    new PrepareStatementCache(config)

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
