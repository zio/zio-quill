package io.getquill

import com.datastax.driver.core.{ Cluster, _ }
import io.getquill.context.cassandra.{ CassandraSessionContext, PrepareStatementCache }
import io.getquill.context.cassandra.util.FutureConversions._
import io.getquill.util.Messages.fail

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }

abstract class CassandraClusterSessionContext[N <: NamingStrategy](
  val naming:                 N,
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends CassandraSessionContext[N] {

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

  def prepare(cql: String): BoundStatement =
    preparedStatementCache(cql)(session.prepare)

  protected def prepareAsync(cql: String)(implicit executionContext: ExecutionContext): Future[BoundStatement] =
    preparedStatementCache.async(cql)(session.prepareAsync(_).asScala)

  def close(): Unit = {
    session.close()
    cluster.close()
  }
}
