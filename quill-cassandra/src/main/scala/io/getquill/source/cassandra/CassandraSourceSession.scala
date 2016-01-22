package io.getquill.source.cassandra

import scala.util.Try

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.Row
import com.datastax.driver.core.Session
import com.typesafe.scalalogging.StrictLogging

import io.getquill.naming.NamingStrategy
import io.getquill.source.cassandra.cluster.ClusterSession
import io.getquill.source.cassandra.encoding.Decoders
import io.getquill.source.cassandra.encoding.Encoders

trait CassandraSourceSession[N <: NamingStrategy]
    extends CassandraSource[N, Row, BoundStatement]
    with StrictLogging
    with Encoders
    with Decoders {

  protected val session: Session =
    ClusterSession(config)

  protected val preparedStatementCache =
    new PrepareStatementCache(config)

  protected def consistencyLevel: ConsistencyLevel =
    session.getCluster.getConfiguration.getQueryOptions.getConsistencyLevel

  protected def prepare(cql: String): BoundStatement = {
    val ps = preparedStatementCache(cql)(session.prepare)
    ps.setConsistencyLevel(consistencyLevel)
    ps
  }

  protected def prepare(cql: String, bind: BoundStatement => BoundStatement): BoundStatement = {
    bind(prepare(cql))
  }

  def close() = session.close

  def probe(cql: String) =
    Try {
      prepare(cql)
      ()
    }
}
