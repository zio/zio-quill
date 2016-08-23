package io.getquill.context.cassandra

import scala.util.Try

import org.slf4j.LoggerFactory

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Row
import com.typesafe.scalalogging.Logger

import io.getquill.CassandraContextConfig
import io.getquill.NamingStrategy
import io.getquill.context.cassandra.encoding.Decoders
import io.getquill.context.cassandra.encoding.Encoders
import io.getquill.util.Messages.fail

abstract class CassandraSessionContext[N <: NamingStrategy](config: CassandraContextConfig)
  extends CassandraContext[N]
  with Encoders
  with Decoders {

  override type PrepareRow = BoundStatement
  override type ResultRow = Row

  override type RunActionReturningResult[T] = Unit
  override type RunBatchActionReturningResult[T] = Unit

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[CassandraSessionContext[_]]))

  private val cluster = config.cluster
  protected val session = cluster.connect(config.keyspace)

  private val preparedStatementCache =
    new PrepareStatementCache(config.preparedStatementCacheSize)

  protected def prepare(cql: String): BoundStatement =
    preparedStatementCache(cql)(session.prepare)

  def close() = {
    session.close
    cluster.close
  }

  def probe(cql: String) =
    Try {
      prepare(cql)
      ()
    }

  def executeActionReturning[O](sql: String, prepare: BoundStatement => BoundStatement = identity, extractor: Row => O, returningColumn: String): Unit =
    fail("Cassandra doesn't support `returning`.")

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Row => T): Unit =
    fail("Cassandra doesn't support `returning`.")
}
