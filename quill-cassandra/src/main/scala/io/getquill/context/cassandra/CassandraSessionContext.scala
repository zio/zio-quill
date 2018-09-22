package io.getquill.context.cassandra

import com.datastax.driver.core.{ Cluster, _ }
import io.getquill.NamingStrategy
import io.getquill.context.Context
import io.getquill.context.cassandra.encoding.{ CassandraTypes, Decoders, Encoders, UdtEncoding }
import io.getquill.util.Messages.fail
import io.getquill.context.cassandra.util.FutureConversions._
import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

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
  with CassandraTypes
  with UdtEncoding {

  val idiom = CqlIdiom

  override type PrepareRow = BoundStatement
  override type ResultRow = Row

  override type RunActionReturningResult[T] = Unit
  override type RunBatchActionReturningResult[T] = Unit

  private val preparedStatementCache =
    new PrepareStatementCache(preparedStatementCacheSize)

  protected lazy val session = cluster.connect(keyspace)

  protected val udtMetadata: Map[String, List[UserType]] = cluster.getMetadata.getKeyspaces.asScala.toList
    .flatMap(_.getUserTypes.asScala)
    .groupBy(_.getTypeName)

  def udtValueOf(udtName: String, keyspace: Option[String] = None): UDTValue =
    udtMetadata.getOrElse(udtName.toLowerCase, Nil) match {
      case udt :: Nil => udt.newValue()
      case Nil =>
        fail(s"Could not find UDT `$udtName` in any keyspace")
      case udts => udts
        .find(udt => keyspace.contains(udt.getKeyspace) || udt.getKeyspace == session.getLoggedKeyspace)
        .map(_.newValue())
        .getOrElse(fail(s"Could not determine to which keyspace `$udtName` UDT belongs. " +
          s"Please specify desired keyspace using UdtMeta"))
    }

  protected def prepare(cql: String): BoundStatement =
    preparedStatementCache(cql)(session.prepare)

  protected def prepareAsync(cql: String)(implicit executionContext: ExecutionContext): Future[BoundStatement] =
    preparedStatementCache.async(cql)(session.prepareAsync(_))

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
