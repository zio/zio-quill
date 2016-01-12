package io.getquill.source.cassandra

import com.datastax.driver.core.Session
import scala.util.Try

trait CassandraSourceSession {
  this: CassandraSource[_, _, _] =>

  protected val session: Session = ClusterSession(config)

  protected val prepare =
    new PrepareStatement(session.prepare, config.getLong("preparedStatementCacheSize"))

  def close() = session.close

  def probe(cql: String) =
    Try {
      prepare(cql)
      ()
    }
}
