package io.getquill.context.monix

import java.sql.{ Array => _ }

import io.getquill.context.StreamingContext
import io.getquill.context.monix.MonixNdbcContext.Runner
import io.getquill.context.ndbc.NdbcContextBase
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.ndbc.TraneFutureConverters
import io.getquill.ndbc.TraneFutureConverters._
import io.getquill.util.ContextLogger
import io.getquill.{ NamingStrategy, ReturnAction }
import io.trane.future.scala.Future
import io.trane.ndbc.{ DataSource, PreparedStatement, Row }
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable

import scala.concurrent.Promise
import scala.concurrent.duration.Duration

object MonixNdbcContext {
  trait Runner {
    /** The schedule method can be used to change the scheduler that this task should be run on */
    def schedule[T](t: Task[T]): Task[T]
    /** The schedule method can be used to change the scheduler that this observable should be observed on */
    def schedule[T](o: Observable[T]): Observable[T]
  }

  object Runner {
    def default = new Runner {
      override def schedule[T](t: Task[T]): Task[T] = t
      override def schedule[T](o: Observable[T]): Observable[T] = o
    }

    def using(scheduler: Scheduler) = new Runner {
      override def schedule[T](t: Task[T]): Task[T] = t.executeOn(scheduler, forceAsync = true)

      override def schedule[T](o: Observable[T]): Observable[T] = o.executeOn(scheduler, forceAsync = true)
    }
  }

  object ContextEffect extends NdbcContextBase.ContextEffect[Task, Scheduler] {
    override def wrapAsync[T](f: (Complete[T]) => Unit): Task[T] = Task.deferFuture {
      val p = Promise[T]()
      f { complete =>
        p.complete(complete)
        ()
      }
      p.future
    }

    override def toFuture[T](eff: Task[T], ec: Scheduler): Future[T] = {
      TraneFutureConverters.scalaToTraneScala(eff.runToFuture(ec))(ec)
    }

    override def fromDeferredFuture[T](f: Scheduler => Future[T]): Task[T] = Task.deferFutureAction(f(_))

    override def flatMap[A, B](a: Task[A])(f: A => Task[B]): Task[B] = a.flatMap(f)

    override def runBlocking[T](eff: Task[T], timeout: Duration): T = {
      import monix.execution.Scheduler.Implicits.global
      eff.runSyncUnsafe(timeout)
    }

    override def wrap[T](t: => T): Task[T] = Task.apply(t)

    override def push[A, B](fa: Task[A])(f: A => B): Task[B] = fa.map(f)

    override def seq[T](f: List[Task[T]]): Task[List[T]] = Task.sequence(f)
  }
}

/**
 * Quill context that wraps all NDBC calls in `monix.eval.Task`.
 *
 */
abstract class MonixNdbcContext[Dialect <: SqlIdiom, Naming <: NamingStrategy, P <: PreparedStatement, R <: Row](
  dataSource: DataSource[P, R],
  runner:     Runner
) extends MonixContext[Dialect, Naming]
  with NdbcContextBase[Dialect, Naming, P, R]
  with StreamingContext[Dialect, Naming]
  with MonixTranslateContext {

  import runner._

  override private[getquill] val logger = ContextLogger(classOf[MonixNdbcContext[_, _, _, _]])

  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  override implicit protected val resultEffect: NdbcContextBase.ContextEffect[Task, Scheduler] = MonixNdbcContext.ContextEffect

  def close(): Unit = {
    dataSource.close()
    ()
  }

  // Need explicit return-type annotations due to scala/bug#8356. Otherwise macro system will not understand Result[Long]=Task[Long] etc...
  override def executeAction[T](sql: String, prepare: Prepare = identityPrepare): Task[Long] =
    super.executeAction(sql, prepare)

  override def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Task[List[T]] =
    super.executeQuery(sql, prepare, extractor)

  override def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Task[T] =
    super.executeQuerySingle(sql, prepare, extractor)

  override def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction): Task[O] =
    super.executeActionReturning(sql, prepare, extractor, returningBehavior)

  override def executeBatchAction(groups: List[BatchGroup]): Task[List[Long]] =
    super.executeBatchAction(groups)

  override def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): Task[List[T]] =
    super.executeBatchActionReturning(groups, extractor)

  override def transaction[T](f: => Task[T]): Task[T] = super.transaction(f)

  override protected def withDataSource[T](f: DataSource[P, R] => Task[T]): Task[T] =
    schedule(f(dataSource))

  protected def withDataSourceObservable[T](f: DataSource[P, R] => Observable[T]): Observable[T] =
    schedule(f(dataSource))

  // TODO: What about fetchSize? Not really applicable here
  def streamQuery[T](fetchSize: Option[Index], sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Observable[T] =
    Observable
      .eval {
        // TODO: Do we need to set ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY?
        val stmt = createPreparedStatement(sql)
        val (params, ps) = prepare(stmt)
        logger.logQuery(sql, params)
        ps
      }
      .flatMap(ps => withDataSourceObservable { ds =>
        Observable.fromReactivePublisher(ds.stream(ps))
      })
      .map(extractor)

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Task[Seq[String]] = {
    withDataSource { _ =>
      resultEffect.wrap(prepare(createPreparedStatement(statement))._1.reverse.map(prepareParam))
    }
  }
}
