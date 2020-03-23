package io.getquill

import com.datastax.driver.core.{ Cluster, ResultSet, Row }
import com.typesafe.config.Config
import io.getquill.context.cassandra.util.FutureConversions._
import io.getquill.util.{ ContextLogger, LoadConfig }
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits
import monix.reactive.Observable

import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success }

class CassandraStreamContext[N <: NamingStrategy](
  naming:                     N,
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends CassandraClusterSessionContext[N](naming, cluster, keyspace, preparedStatementCacheSize) {

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
      Task.fromFuture(rs.fetchMoreResults().asScala(Implicits.global)).map(_ => page)
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Observable[T] = {

    Observable
      .fromTask(prepareRowAndLog(cql, prepare))
      .mapEvalF(p => session.executeAsync(p).asScala(Implicits.global))
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
      .mapEvalF(p => session.executeAsync(p).asScala(Implicits.global))
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
    Task.async0[PrepareRow] { (scheduler, callback) =>
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
    }
  }
}
