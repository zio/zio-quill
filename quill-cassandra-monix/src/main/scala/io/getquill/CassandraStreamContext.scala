package io.getquill

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{ AsyncResultSet, ResultSet, Row }
import com.typesafe.config.Config
import io.getquill.context.ExecutionInfo
import io.getquill.context.cassandra.util.FutureConversions._
import io.getquill.util.{ ContextLogger, LoadConfig }
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits
import monix.reactive.Observable

import scala.compat.java8.FutureConverters._
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success }

class CassandraStreamContext[N <: NamingStrategy](
  naming:                     N,
  session:                    CqlSession,
  preparedStatementCacheSize: Long
)
  extends CassandraCqlSessionContext[N](naming, session, preparedStatementCacheSize) {

  def this(naming: N, config: CassandraContextConfig) = this(naming, config.session, config.preparedStatementCacheSize)
  def this(naming: N, config: Config) = this(naming, CassandraContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  private val logger = ContextLogger(classOf[CassandraStreamContext[_]])

  override type Result[T] = Observable[T]
  override type RunQueryResult[T] = T
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Unit
  override type RunBatchActionResult = Unit

  protected def page(rs: AsyncResultSet): Task[Iterable[Row]] = Task.defer {
    val page = rs.currentPage().asScala
    if (rs.hasMorePages)
      Task.from(rs.fetchNextPage().toCompletableFuture.toScala).map(_ => page)
    else
      Task.now(page)
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner): Observable[T] = {
    Observable
      .fromTask(prepareRowAndLog(cql, prepare))
      .mapEvalF(p => session.executeAsync(p).toScala)
      .flatMap(Observable.fromAsyncStateAction((rs: AsyncResultSet) => page(rs).map((_, rs)))(_))
      .takeWhile(_.nonEmpty)
      .flatMap(Observable.fromIterable)
      .map(row => extractor(row, this))
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner): Observable[T] =
    executeQuery(cql, prepare, extractor)(info, dc)

  def executeAction(cql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner): Observable[Unit] = {
    Observable
      .fromTask(prepareRowAndLog(cql, prepare))
      .mapEvalF(p => session.executeAsync(p).toScala)
      .map(_ => ())
  }

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner): Observable[Unit] =
    Observable.fromIterable(groups).flatMap {
      case BatchGroup(cql, prepare) =>
        Observable.fromIterable(prepare)
          .flatMap(executeAction(cql, _)(info, dc))
          .map(_ => ())
    }

  private def prepareRowAndLog(cql: String, prepare: Prepare = identityPrepare): Task[PrepareRow] = {
    Task.async0[PrepareRow] { (scheduler, callback) =>
      implicit val executor: Scheduler = scheduler

      super.prepareAsync(cql)
        .map(row => prepare(row, this))
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
