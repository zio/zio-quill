package io.getquill

import com.datastax.driver.core.{ Cluster, _ }
import io.getquill.context.cassandra.util.FutureConversions._
import io.getquill.context.cassandra.{ CassandraSessionContext, PrepareStatementCache }
import io.getquill.util.Messages.fail

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Failure

abstract class CassandraClusterSessionContext[N <: NamingStrategy](
  val naming:                 N,
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends CassandraSessionContext[N] {

  private lazy val asyncCache = new PrepareStatementCache[Future[PreparedStatement]](preparedStatementCacheSize)
  private lazy val syncCache = new PrepareStatementCache[PreparedStatement](preparedStatementCacheSize)

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
    syncCache(cql)(stmt => session.prepare(stmt)).bind()

  protected def prepareAsync(cql: String)(implicit executionContext: ExecutionContext): Future[BoundStatement] =
    asyncCache(cql)(stmt => session.prepareAsync(stmt).asScala andThen {
      case Failure(_) => asyncCache.invalidate(stmt)
    }).map(_.bind())

  def close(): Unit = {
    session.close()
    cluster.close()
  }
}
