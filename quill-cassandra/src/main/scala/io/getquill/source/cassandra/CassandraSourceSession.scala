package io.getquill.source.cassandra

import com.datastax.driver.core.Session
import scala.util.Try
import com.datastax.driver.core.ConsistencyLevel

trait CassandraSourceSession {
  this: CassandraSource[_, _, _] =>

  protected lazy val session: Session = ClusterSession(config)

  protected def queryConsistencyLevel: Option[ConsistencyLevel] = None

  private def prepareStmt(cql: String) = {
    val ps = session.prepare(cql)
    queryConsistencyLevel.map(ps.setConsistencyLevel)
    ps
  }

  protected val prepare =
    new PrepareStatement(prepareStmt, config.getLong("preparedStatementCacheSize"))

  def close() = session.close

  def probe(cql: String) =
    Try {
      prepare(cql)
      ()
    }
}
