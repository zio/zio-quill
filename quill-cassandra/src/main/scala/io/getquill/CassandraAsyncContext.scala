package io.getquill

import com.datastax.driver.core.Cluster
import com.typesafe.config.Config
import io.getquill.context.cassandra.util.FutureConversions._
import io.getquill.monad.ScalaFutureIOMonad
import io.getquill.util.{ ContextLogger, LoadConfig }

import scala.jdk.CollectionConverters._
import scala.concurrent.{ ExecutionContext, Future }

class CassandraAsyncContext[N <: NamingStrategy](
  naming:                     N,
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends CassandraClusterSessionContext[N](naming, cluster, keyspace, preparedStatementCacheSize)
  with ScalaFutureIOMonad {

  def this(naming: N, config: CassandraContextConfig) = this(naming, config.cluster, config.keyspace, config.preparedStatementCacheSize)

  def this(naming: N, config: Config) = this(naming, CassandraContextConfig(config))

  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  private val logger = ContextLogger(classOf[CassandraAsyncContext[_]])

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Unit
  override type RunBatchActionResult = Unit

  override def performIO[T](io: IO[T, _], transactional: Boolean = false)(implicit ec: ExecutionContext): Result[T] = {
    if (transactional) logger.underlying.warn("Cassandra doesn't support transactions, ignoring `io.transactional`")
    super.performIO(io)
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit executionContext: ExecutionContext): Result[RunQueryResult[T]] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, logger)
    statement.flatMap(st => session.executeAsync(st).asScala)
      .map(_.all.asScala.toList.map(extractor))
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit executionContext: ExecutionContext): Result[RunQuerySingleResult[T]] = {
    executeQuery(cql, prepare, extractor).map(handleSingleResult)
  }

  def executeAction[T](cql: String, prepare: Prepare = identityPrepare)(implicit executionContext: ExecutionContext): Result[RunActionResult] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, logger)
    statement.flatMap(st => session.executeAsync(st).asScala).map(_ => ())
  }

  def executeBatchAction(groups: List[BatchGroup])(implicit executionContext: ExecutionContext): Result[RunBatchActionResult] = {
    Future.sequence {
      groups.flatMap {
        case BatchGroup(cql, prepare) =>
          prepare.map(executeAction(cql, _))
      }
    }.map(_ => ())
  }
}
