package io.getquill

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Row
import io.getquill.context.cassandra.util.FutureConversions.toScalaFuture
import io.getquill.util.LoadConfig
import com.typesafe.config.Config
import scala.collection.JavaConverters._
import io.getquill.context.cassandra.CassandraSessionContext

class CassandraAsyncContext[N <: NamingStrategy](config: CassandraContextConfig)
  extends CassandraSessionContext[N](config) {

  def this(config: Config) = this(CassandraContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override type RunQueryResult[T] = Future[List[T]]
  override type RunQuerySingleResult[T] = Future[T]
  override type RunActionResult = Future[Unit]
  override type RunBatchActionResult = Future[Unit]

  def executeQuery[T](cql: String, prepare: BoundStatement => BoundStatement = identity, extractor: Row => T = identity[Row] _)(implicit ec: ExecutionContext): Future[List[T]] =
    session.executeAsync(prepare(super.prepare(cql)))
      .map(_.all.asScala.toList.map(extractor))

  def executeQuerySingle[T](cql: String, prepare: BoundStatement => BoundStatement = identity, extractor: Row => T = identity[Row] _)(implicit ec: ExecutionContext): Future[T] =
    executeQuery(cql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](cql: String, prepare: BoundStatement => BoundStatement = identity)(implicit ec: ExecutionContext): Future[Unit] =
    session.executeAsync(prepare(super.prepare(cql))).map(_ => ())

  def executeBatchAction(groups: List[BatchGroup])(implicit ec: ExecutionContext): Future[Unit] =
    Future.sequence {
      groups.map {
        case BatchGroup(cql, prepare) =>
          prepare.map(executeAction(cql, _))
      }.flatten
    }.map(_ => ())
}
