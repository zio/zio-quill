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
import monix.execution.{ Cancelable, Scheduler }

import scala.util.{ Failure, Success }

class CassandraStreamContext[N <: NamingStrategy](
  naming:                     N,
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends CassandraSessionContext[N](naming, cluster, keyspace, preparedStatementCacheSize) {

  def this(naming: N, config: CassandraContextConfig) = this(naming, config.cluster, config.keyspace, config.preparedStatementCacheSize)
  def this(naming: N, config: Config) = this(naming, CassandraContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  private val logger = ContextLogger(classOf[CassandraStreamContext[_]])

  override type Result[T] = Observable[T]
  override type RunQueryResult[T] = T
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Unit
  override type RunBatchActionResult = Unit

  protected def page(rs: ResultSet): Task[Iterable[Row]] = Task.defer {
    val available = rs.getAvailableWithoutFetching
    val page = rs.asScala.take(available)

    if (rs.isFullyFetched)
      Task.now(page)
    else
      Task.fromFuture(rs.fetchMoreResults()).map(_ => page)
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Observable[T] = {

    Observable
      .fromTask(prepareRowAndLog(cql, prepare))
      .mapFuture(session.executeAsync(_))
      .flatMap(Observable.fromAsyncStateAction((rs: ResultSet) => page(rs).map((_, rs)))(_))
      .takeWhile(_.nonEmpty)
      .flatMap(Observable.fromIterable)
      .map(extractor)
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Observable[T] =
    executeQuery(cql, prepare, extractor)

  def executeAction[T](cql: String, prepare: Prepare = identityPrepare): Observable[Unit] = {
    Observable
      .fromTask(prepareRowAndLog(cql, prepare))
      .mapFuture(session.executeAsync(_))
      .map(_ => ())
  }

  def executeBatchAction(groups: List[BatchGroup]): Observable[Unit] =
    Observable.fromIterable(groups).flatMap {
      case BatchGroup(cql, prepare) =>
        Observable.fromIterable(prepare)
          .flatMap(executeAction(cql, _))
          .map(_ => ())
    }

  private def prepareRowAndLog(cql: String, prepare: Prepare = identityPrepare): Task[PrepareRow] = {
    Task.async[PrepareRow] { (scheduler, callback) =>
      implicit val executor: Scheduler = scheduler

      super.prepareAsync(cql)
        .map(prepare)
        .onComplete {
          case Success((params, bs)) =>
            logger.logQuery(cql, params)
            callback.onSuccess(bs)
          case Failure(ex) =>
            callback.onError(ex)
        }

      Cancelable.empty
    }
  }
}
