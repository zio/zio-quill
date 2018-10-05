package io.getquill

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import io.getquill.context.cassandra.util.FutureConversions.toScalaFuture
import io.getquill.util.{ ContextLogger, LoadConfig }
import com.typesafe.config.Config
import scala.collection.JavaConverters._
import io.getquill.context.cassandra.CassandraSessionContext
import com.datastax.driver.core.Cluster
import io.getquill.monad.ScalaFutureIOMonad

class CassandraAsyncContext[N <: NamingStrategy](
  naming:                     N,
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends CassandraSessionContext[N](naming, cluster, keyspace, preparedStatementCacheSize)
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

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext): Future[List[T]] =
    this.prepareAsync(cql).map(prepare).flatMap {
      case (params, bs) =>
        logger.logQuery(cql, params)
        session.executeAsync(bs)
          .map(_.all.asScala.toList.map(extractor))
    }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext): Future[T] =
    executeQuery(cql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](cql: String, prepare: Prepare = identityPrepare)(implicit ec: ExecutionContext): Future[Unit] = {
    this.prepareAsync(cql).map(prepare).flatMap {
      case (params, bs) =>
        logger.logQuery(cql, params)
        session.executeAsync(bs).map(_ => ())
    }
  }

  def executeBatchAction(groups: List[BatchGroup])(implicit ec: ExecutionContext): Future[Unit] =
    Future.sequence {
      groups.flatMap {
        case BatchGroup(cql, prepare) =>
          prepare.map(executeAction(cql, _))
      }
    }.map(_ => ())
}
