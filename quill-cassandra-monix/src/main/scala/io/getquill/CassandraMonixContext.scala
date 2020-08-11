package io.getquill

import com.datastax.driver.core.{ Cluster, ResultSet, Row }
import com.typesafe.config.Config
import io.getquill.context.cassandra.CqlIdiom
import io.getquill.context.monix.MonixContext
import io.getquill.util.{ ContextLogger, LoadConfig }
import io.getquill.context.cassandra.util.FutureConversions._
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable

import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success }

class CassandraMonixContext[N <: NamingStrategy](
  naming:                     N,
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends CassandraClusterSessionContext[N](naming, cluster, keyspace, preparedStatementCacheSize)
  with MonixContext[CqlIdiom, N] {

  def this(naming: N, config: CassandraContextConfig) = this(naming, config.cluster, config.keyspace, config.preparedStatementCacheSize)
  def this(naming: N, config: Config) = this(naming, CassandraContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  private val logger = ContextLogger(classOf[CassandraMonixContext[_]])

  override type StreamResult[T] = Observable[T]
  override type RunActionResult = Unit
  override type Result[T] = Task[T]

  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunBatchActionResult = Unit

  protected def page(rs: ResultSet): Task[Iterable[Row]] = Task.defer {
    val available = rs.getAvailableWithoutFetching
    val page = rs.asScala.take(available)

    if (rs.isFullyFetched)
      Task.now(page)
    else
      Task.fromFuture(rs.fetchMoreResults().asScalaWithDefaultGlobal).map(_ => page)
  }

  def streamQuery[T](fetchSize: Option[Int], cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Observable[T] = {

    Observable
      .fromTask(prepareRowAndLog(cql, prepare))
      .mapEvalF(p => session.executeAsync(p).asScalaWithDefaultGlobal)
      .flatMap(Observable.fromAsyncStateAction((rs: ResultSet) => page(rs).map((_, rs)))(_))
      .takeWhile(_.nonEmpty)
      .flatMap(Observable.fromIterable)
      .map(extractor)
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Task[List[T]] = {
    streamQuery[T](None, cql, prepare, extractor)
      .foldLeftL(List[T]())({ case (l, r) => r +: l }).map(_.reverse)
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Task[T] =
    executeQuery(cql, prepare, extractor).map(handleSingleResult(_))

  def executeAction[T](cql: String, prepare: Prepare = identityPrepare): Task[Unit] = {
    prepareRowAndLog(cql, prepare)
      .flatMap(r => Task.fromFuture(session.executeAsync(r).asScalaWithDefaultGlobal))
      .map(_ => ())
  }

  def executeBatchAction(groups: List[BatchGroup]): Task[Unit] =
    Observable.fromIterable(groups).flatMap {
      case BatchGroup(cql, prepare) =>
        Observable.fromIterable(prepare)
          .flatMap(prep => Observable.fromTask(executeAction(cql, prep)))
          .map(_ => ())
    }.completedL

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
