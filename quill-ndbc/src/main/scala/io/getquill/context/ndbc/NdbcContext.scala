package io.getquill.context.ndbc

import io.getquill.context.TranslateContextBase
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.{ NamingStrategy, ReturnAction }
import io.trane.future.scala.{ Await, Future, Promise }
import io.trane.ndbc.{ DataSource, PreparedStatement, Row }

import scala.concurrent.duration.Duration

abstract class NdbcContext[I <: SqlIdiom, N <: NamingStrategy, P <: PreparedStatement, R <: Row](
  override val idiom: I, override val naming: N, val dataSource: DataSource[P, R]
)
  extends NdbcContextBase[I, N, P, R]
  with TranslateContextBase {

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  override implicit protected val resultEffect: NdbcContextBase.ContextEffect[Future, Unit] = NdbcContext.ContextEffect

  override type TranslateResult[T] = Future[T]

  override private[getquill] val translateEffect = resultEffect

  // Need explicit return-type annotations due to scala/bug#8356. Otherwise macro system will not understand Result[Long]=Long etc...
  override def executeAction[T](sql: String, prepare: Prepare = identityPrepare): Future[Long] =
    super.executeAction(sql, prepare)

  override def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Future[List[T]] =
    super.executeQuery(sql, prepare, extractor)

  override def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Future[T] =
    super.executeQuerySingle(sql, prepare, extractor)

  override def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction): Future[O] =
    super.executeActionReturning(sql, prepare, extractor, returningBehavior)

  override def executeBatchAction(groups: List[BatchGroup]): Future[List[Long]] =
    super.executeBatchAction(groups)

  override def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): Future[List[T]] =
    super.executeBatchActionReturning(groups, extractor)

  override def transaction[T](f: => Future[T]): Future[T] = super.transaction(f)

  /* TODO: I'm assuming that we don't need to bracket and close the dataSource like with JDBC
      because previously it wasn't done here either */
  override def withDataSource[T](f: DataSource[P, R] => Future[T]): Future[T] = f(dataSource)

  def close(): Unit = {
    dataSource.close()
    ()
  }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Future[Seq[String]] =
    withDataSource { _ =>
      resultEffect.wrap(prepare(createPreparedStatement(statement))._1.reverse.map(prepareParam))
    }
}

object NdbcContext {
  object ContextEffect extends NdbcContextBase.ContextEffect[Future, Unit] {
    override def wrap[T](t: => T): Future[T] = Future(t)

    //noinspection DuplicatedCode
    override def wrapAsync[T](f: (Complete[T]) => Unit): Future[T] = {
      val p = Promise[T]()
      f { complete =>
        p.complete(complete)
        ()
      }
      p.future
    }

    override def toFuture[T](eff: Future[T], ec: Unit): Future[T] = eff

    override def fromDeferredFuture[T](f: Unit => Future[T]): Future[T] = f(())

    override def push[A, B](a: Future[A])(f: A => B): Future[B] = a.map(f)

    override def flatMap[A, B](a: Future[A])(f: A => Future[B]): Future[B] = a.flatMap(f)

    override def seq[T](list: List[Future[T]]): Future[List[T]] = Future.sequence(list)

    override def runBlocking[T](eff: Future[T], timeout: Duration): T = Await.result(eff, timeout)
  }
}
