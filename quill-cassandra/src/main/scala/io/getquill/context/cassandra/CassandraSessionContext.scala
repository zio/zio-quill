package io.getquill.context.cassandra

import scala.util.Try
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Row
import io.getquill.NamingStrategy
import io.getquill.context.cassandra.encoding.{ CassandraTypes, Decoders, Encoders }
import io.getquill.util.Messages.fail
import com.datastax.driver.core.Cluster
import io.getquill.context.Context

abstract class CassandraSessionContext[N <: NamingStrategy](
  val naming:                 N,
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends Context[CqlIdiom, N]
  with CassandraContext[N]
  with Encoders
  with Decoders
  with CassandraTypes {

  val idiom = CqlIdiom

  override type PrepareRow = BoundStatement
  override type ResultRow = Row

  override type RunActionReturningResult[T] = Unit
  override type RunBatchActionReturningResult[T] = Unit

  private val preparedStatementCache =
    new PrepareStatementCache(preparedStatementCacheSize)

  protected val session = cluster.connect(keyspace)

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

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningColumn: String): Unit =
    fail("Cassandra doesn't support `returning`.")

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): Unit =
    fail("Cassandra doesn't support `returning`.")
}
