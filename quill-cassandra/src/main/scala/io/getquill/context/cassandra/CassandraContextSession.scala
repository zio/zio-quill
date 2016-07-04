package io.getquill.context.cassandra

import scala.util.Try
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Row
import io.getquill.NamingStrategy
import io.getquill.context.cassandra.encoding.Decoders
import io.getquill.context.cassandra.encoding.Encoders
import io.getquill.context.BindedStatementBuilder
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import io.getquill.CassandraContextConfig

abstract class CassandraContextSession[N <: NamingStrategy](config: CassandraContextConfig)
  extends CassandraContext[N, Row, BindedStatementBuilder[BoundStatement]]
  with Encoders
  with Decoders {

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[CassandraContextSession[_]]))

  private val cluster = config.cluster
  protected val session = cluster.connect(config.keyspace)

  protected val preparedStatementCache =
    new PrepareStatementCache(config.preparedStatementCacheSize)

  protected def prepare(cql: String): BoundStatement =
    preparedStatementCache(cql)(session.prepare)

  protected def prepare(cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement]): BoundStatement = {
    val (expanded, set) = bind(new BindedStatementBuilder[BoundStatement]).build(cql)
    logger.info(expanded)
    set(prepare(expanded))
  }

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
