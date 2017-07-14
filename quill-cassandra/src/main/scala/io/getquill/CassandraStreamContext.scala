package io.getquill

import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import com.typesafe.config.Config

import scala.collection.JavaConverters._
import io.getquill.context.cassandra.CassandraSessionContext
import io.getquill.context.cassandra.util.FutureConversions.toScalaFuture
import monix.reactive.Observable
import io.getquill.util.{ ContextLogger, LoadConfig }
import com.datastax.driver.core.Cluster
import monix.eval.Task

class CassandraStreamContext[N <: NamingStrategy](
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends CassandraSessionContext[N](cluster, keyspace, preparedStatementCacheSize) {

  def this(config: CassandraContextConfig) = this(config.cluster, config.keyspace, config.preparedStatementCacheSize)
  def this(config: Config) = this(CassandraContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  private val logger = ContextLogger(classOf[CassandraStreamContext[_]])

  override type RunQueryResult[T] = Observable[T]
  override type RunQuerySingleResult[T] = Observable[T]
  override type RunActionResult = Observable[Unit]
  override type RunBatchActionResult = Observable[Unit]

  protected def page(rs: ResultSet): Task[Iterable[Row]] = Task.defer {
    val available = rs.getAvailableWithoutFetching
    val page = rs.asScala.take(available)

    if (rs.isFullyFetched)
      Task.now(page)
    else
      Task.fromFuture(rs.fetchMoreResults()).map(_ => page)
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Observable[T] = {
    val (params, bs) = prepare(super.prepare(cql))
    logger.logQuery(cql, params)
    Observable
      .fromFuture(session.executeAsync(bs))
      .flatMap(Observable.fromAsyncStateAction((rs: ResultSet) => page(rs).map((_, rs)))(_))
      .takeWhile(_.nonEmpty)
      .flatMap(Observable.fromIterable)
      .map(extractor)
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Observable[T] =
    executeQuery(cql, prepare, extractor)

  def executeAction[T](cql: String, prepare: Prepare = identityPrepare): Observable[Unit] = {
    val (params, bs) = prepare(super.prepare(cql))
    logger.logQuery(cql, params)
    Observable.fromFuture(session.executeAsync(bs)).map(_ => ())
  }

  def executeBatchAction(groups: List[BatchGroup]): Observable[Unit] =
    Observable.fromIterable(groups).flatMap {
      case BatchGroup(cql, prepare) =>
        Observable.fromIterable(prepare)
          .flatMap(executeAction(cql, _))
          .map(_ => ())
    }
}
