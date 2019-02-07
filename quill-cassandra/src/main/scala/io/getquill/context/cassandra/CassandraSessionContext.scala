package io.getquill.context.cassandra

import com.datastax.driver.core.{Cluster, _}
import io.getquill.NamingStrategy
import io.getquill.context.Context
import io.getquill.context.cassandra.encoding.{CassandraTypes, Decoders, Encoders, UdtEncoding}
import io.getquill.util.ContextLogger
import io.getquill.util.Messages.fail

import scala.collection.JavaConverters._
import scala.util.Try

abstract class CassandraSessionContext[N <: NamingStrategy]
  extends Context[CqlIdiom, N]
  with CassandraContext[N]
  with Encoders
  with Decoders
  with CassandraTypes
  with UdtEncoding {

  val naming:  N
  val cluster: Cluster
  val keyspace: String
  val preparedStatementCacheSize: Long

  val idiom = CqlIdiom

  private val logger = ContextLogger(classOf[CassandraSessionContext[_]])

  override type PrepareRow = BoundStatement
  override type ResultRow = Row

  type RunContext

  // It can be assumed that all cassandra output contexts will be a singleton-type but it cannot be assumed that it will be scala.Unit.
  // other frameworks (e.g. Lagom) may have their own singleton-type.
  type Completed
  override type RunActionReturningResult[T] = Completed
  override type RunBatchActionReturningResult[T] = Completed

  def complete:Completed

  protected val effect: CassandraContextEffect[Result, RunContext]
  val withContextActions = effect.withContextActions
  import effect.ImplicitsWithContext._
  import withContextActions._

  private val preparedStatementCache =
    new PrepareStatementCache(preparedStatementCacheSize, effect)

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

  protected def prepareAsync(cql: String)(implicit executionContext: RunContext): Result[BoundStatement] =
    preparedStatementCache.async(cql)(prep => wrapListenableFuture(session.prepareAsync(prep)))

  def close() = {
    session.close
    cluster.close
  }

  def probe(cql: String) =
    Try {
      prepare(cql)
      ()
    }

  // For now leave the IO monad to specifically use scala.Future and not context effects. Maybe in a future refactor...
  //  override def performIO[T](io: IO[T, _], transactional: Boolean = false)(implicit ec: ExecutionContext): Result[T] = {
  //    if (transactional) logger.underlying.warn("Cassandra doesn't support transactions, ignoring `io.transactional`")
  //    super.performIO(io)
  //  }

  def executeQuery[T](cql: String, prepare: Prepare, extractor: Extractor[T])(implicit ec: RunContext): Result[List[T]] = {
    this.prepareAsync(cql).map(prepare).flatMap {
      case (params, bs) =>
        logger.logQuery(cql, params)
        wrapListenableFuture(session.executeAsync(bs))
          .map(_.all.asScala.toList.map(extractor))
    }
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare, extractor: Extractor[T])(implicit ec: RunContext): Result[T] =
    executeQuery(cql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](cql: String, prepare: Prepare)(implicit ec: RunContext): Result[Completed] = {
    this.prepareAsync(cql).map(prepare).flatMap {
      case (params, bs) =>
        logger.logQuery(cql, params)
        wrapListenableFuture(session.executeAsync(bs)).map(_ => complete)
    }
  }

  def executeBatchAction(groups: List[BatchGroup])(implicit ec: RunContext): Result[Completed] =
    seq {
      groups.flatMap {
        case BatchGroup(cql, prepare) =>
          prepare.map(executeAction(cql, _))
      }
    }.map(_ => complete)

  def executeActionReturning[O](sql: String, prepare: Prepare, extractor: Extractor[O], returningColumn: String): Completed =
    fail("Cassandra doesn't support `returning`.")

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): Completed =
    fail("Cassandra doesn't support `returning`.")
}
