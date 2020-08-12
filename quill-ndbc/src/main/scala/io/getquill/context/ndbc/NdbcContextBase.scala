package io.getquill.context.ndbc

import java.util
import java.util.concurrent.Executors
import java.util.function.Supplier

import io.getquill._
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.ndbc.TraneFutureConverters._
import io.getquill.util.ContextLogger
import io.trane.future.FuturePool
import io.trane.future.scala.{ Future, toScalaFuture }
import io.trane.ndbc.{ DataSource, PreparedStatement, Row }

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.language.{ higherKinds, implicitConversions }
import scala.util.Try

object NdbcContextBase {
  trait ContextEffect[F[_], FutureExecutionContext_] extends context.ContextEffect[F] {
    final type Complete[T] = (Try[T] => Unit)

    final type FutureExecutionContext = FutureExecutionContext_

    def wrapAsync[T](f: Complete[T] => Unit): F[T]

    def wrapFromFuture[T](fut: Future[T]): F[T] = wrapAsync(fut.onComplete)

    def toFuture[T](eff: F[T], ec: this.FutureExecutionContext): Future[T]

    def fromDeferredFuture[T](f: (this.FutureExecutionContext) => Future[T]): F[T]

    def flatMap[A, B](a: F[A])(f: A => F[B]): F[B]
    def traverse[A, B](list: List[A])(f: A => F[B]) = seq(list.map(f))

    def runBlocking[T](eff: F[T], timeout: Duration): T
  }
}

trait NdbcContextBase[Idiom <: SqlIdiom, Naming <: NamingStrategy, P <: PreparedStatement, R <: Row]
  extends SqlContext[Idiom, Naming] {

  private[getquill] val logger = ContextLogger(classOf[NdbcContext[_, _, _, _]])

  final override type PrepareRow = P
  final override type ResultRow = R

  protected implicit val resultEffect: NdbcContextBase.ContextEffect[Result, _]
  import resultEffect._

  protected def withDataSource[T](f: DataSource[P, R] => Result[T]): Result[T]

  final protected def withDataSourceFromFuture[T](f: DataSource[P, R] => Future[T]): Result[T] =
    withDataSource { ds => resultEffect.wrapFromFuture(f(ds)) }

  protected def createPreparedStatement(sql: String): P

  protected def expandAction(sql: String, returningAction: ReturnAction) = sql

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: R => T = identity[R] _): Result[List[T]] = {
    withDataSourceFromFuture { ds =>
      val (params, ps) = prepare(createPreparedStatement(sql))
      logger.logQuery(sql, params)

      ds.query(ps).toScala.map { rs =>
        extractResult(rs.iterator, extractor)
      }
    }
  }

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: R => T = identity[R] _): Result[T] =
    push(executeQuery(sql, prepare, extractor))(handleSingleResult)

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare): Result[Long] = {
    withDataSourceFromFuture { ds =>
      val (params, ps) = prepare(createPreparedStatement(sql))
      logger.logQuery(sql, params)
      ds.execute(ps).toScala.map(_.longValue)
    }
  }

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: R => O, returningAction: ReturnAction): Result[O] = {
    val expanded = expandAction(sql, returningAction)
    executeQuerySingle(expanded, prepare, extractor)
  }

  def executeBatchAction(groups: List[BatchGroup]): Result[List[Long]] =
    push(
      traverse(groups) {
        case BatchGroup(sql, prepares) =>
          prepares.foldLeft(wrap(ArrayBuffer.empty[Long])) { (acc, prepare) =>
            flatMap(acc) { array =>
              push(executeAction(sql, prepare))(array :+ _)
            }
          }
      }
    )(_.flatten)

  // TODO: Should this be blocking? Previously it was just a Future wrapped in a Try, which makes no sense
  def probe(sql: String): Try[_] =
    Try(runBlocking(withDataSourceFromFuture(_.query(sql).toScala), Duration.Inf))

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: R => T): Result[List[T]] =
    push(
      traverse(groups) {
        case BatchGroupReturning(sql, column, prepare) =>
          prepare.foldLeft(wrap(ArrayBuffer.empty[T])) { (acc, prepare) =>
            flatMap(acc) { array =>
              push(executeActionReturning(sql, prepare, extractor, column))(array :+ _)
            }
          }
      }
    )(_.flatten)

  @tailrec
  private def extractResult[T](rs: util.Iterator[R], extractor: R => T, acc: List[T] = Nil): List[T] =
    if (rs.hasNext)
      extractResult(rs, extractor, extractor(rs.next()) :: acc)
    else
      acc.reverse

  def transaction[T](f: => Result[T]): Result[T] = withDataSource { ds =>
    /* TODO: I'm assuming that we don't need to turn autocommit off/on for streaming because I can't
        find any way to do so with the NDBC DataSource and it seems to handle streaming on its own */

    implicit def javaSupplier[S](s: => S): Supplier[S] = new Supplier[S] {
      override def get = s
    }

    val javaFuturePool = FuturePool.apply(Executors.newCachedThreadPool())

    resultEffect.fromDeferredFuture(implicit scheduler =>
      javaFuturePool.isolate(
        ds.transactional {
          resultEffect.toFuture(f, scheduler).toJava
        }
      ))
  }
}