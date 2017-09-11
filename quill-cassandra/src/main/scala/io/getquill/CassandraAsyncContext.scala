package io.getquill

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import io.getquill.context.cassandra.util.FutureConversions.toScalaFuture
import io.getquill.util.{ ContextLogger, LoadConfig }
import com.typesafe.config.Config
import scala.collection.JavaConverters._
import io.getquill.context.cassandra.CassandraSessionContext
import com.datastax.driver.core.Cluster

class CassandraAsyncContext[N <: NamingStrategy](
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends CassandraSessionContext[N](cluster, keyspace, preparedStatementCacheSize) {

  def this(config: CassandraContextConfig) = this(config.cluster, config.keyspace, config.preparedStatementCacheSize)
  def this(config: Config) = this(CassandraContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  private val logger = ContextLogger(classOf[CassandraAsyncContext[_]])

  override type RunQueryResult[T] = Future[List[T]]
  override type RunQuerySingleResult[T] = Future[T]
  override type RunActionResult = Future[Unit]
  override type RunBatchActionResult = Future[Unit]

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext): Future[List[T]] = {
    val (params, bs) = prepare(super.prepare(cql))
    logger.logQuery(cql, params)
    session.executeAsync(bs)
      .map(_.all.asScala.toList.map(extractor))
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext): Future[T] =
    executeQuery(cql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](cql: String, prepare: Prepare = identityPrepare)(implicit ec: ExecutionContext): Future[Unit] = {
    val (params, bs) = prepare(super.prepare(cql))
    logger.logQuery(cql, params)
    session.executeAsync(bs).map(_ => ())
  }

  def executeBatchAction(groups: List[BatchGroup])(implicit ec: ExecutionContext): Future[Unit] =
    Future.sequence {
      groups.flatMap {
        case BatchGroup(cql, prepare) =>
          prepare.map(executeAction(cql, _))
      }
    }.map(_ => ())
}
