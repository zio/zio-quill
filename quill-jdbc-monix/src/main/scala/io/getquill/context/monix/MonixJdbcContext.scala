package io.getquill.context.monix

import java.io.Closeable
import java.sql.{ Array => _, _ }

import cats.effect.ExitCase
import io.getquill.{ NamingStrategy, ReturnAction }
import io.getquill.context.StreamingContext
import io.getquill.context.jdbc.JdbcContextBase
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.ContextLogger
import javax.sql.DataSource
import monix.eval.{ Task, TaskLocal }
import monix.execution.misc.Local
import monix.reactive.Observable

import scala.util.Try

/**
 * Quill context that wraps all JDBC calls in `monix.eval.Task`.
 *
 */
abstract class MonixJdbcContext[Dialect <: SqlIdiom, Naming <: NamingStrategy](
  dataSource: DataSource with Closeable,
  runner:     Runner
) extends MonixContext[Dialect, Naming]
  with JdbcContextBase[Dialect, Naming]
  with StreamingContext[Dialect, Naming]
  with MonixTranslateContext {

  override private[getquill] val logger = ContextLogger(classOf[MonixJdbcContext[_, _]])

  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

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
  override def prepareQuery[T](sql: String, prepare: Prepare, extractor: Extractor[T] = identityExtractor): Connection => Task[PreparedStatement] =
    super.prepareQuery(sql, prepare, extractor)
  override def prepareAction(sql: String, prepare: Prepare): Connection => Task[PreparedStatement] =
    super.prepareAction(sql, prepare)
  override def prepareBatchAction(groups: List[BatchGroup]): Connection => Task[List[PreparedStatement]] =
    super.prepareBatchAction(groups)

  override protected val effect: Runner = runner
  import runner._

  private val currentConnection: Local[Option[Connection]] = Local(None)

  override def close(): Unit = dataSource.close()

  override protected def withConnection[T](f: Connection => Task[T]): Task[T] =
    for {
      maybeConnection <- wrap { currentConnection() }
      result <- maybeConnection match {
        case Some(connection) => f(connection)
        case None =>
          schedule {
            wrap(dataSource.getConnection).bracket(f)(conn => wrapClose(conn.close()))
          }
      }
    } yield result

  protected def withConnectionObservable[T](f: Connection => Observable[T]): Observable[T] =
    for {
      maybeConnection <- Observable.eval(currentConnection())
      result <- maybeConnection match {
        case Some(connection) =>
          withAutocommitBracket(connection, f)
        case None =>
          scheduleObservable {
            Observable.eval(dataSource.getConnection)
              .bracket(conn => withAutocommitBracket(conn, f))(conn => wrapClose(conn.close()))
          }
      }
    } yield result

  /**
   * Need to store, set and restore the client's autocommit mode since some vendors (e.g. postgres)
   * don't like autocommit=true during streaming sessions. Using brackets to do that.
   */
  private[getquill] def withAutocommitBracket[T](conn: Connection, f: Connection => Observable[T]): Observable[T] = {
    Observable.eval(autocommitOff(conn))
      .bracket({ case (conn, _) => f(conn) })(autoCommitBackOn)
  }

  private[getquill] def withAutocommitBracket[T](conn: Connection, f: Connection => Task[T]): Task[T] = {
    Task(autocommitOff(conn))
      .bracket({ case (conn, _) => f(conn) })(autoCommitBackOn)
  }

  private[getquill] def withCloseBracket[T](conn: Connection, f: Connection => Task[T]): Task[T] = {
    Task(conn)
      .bracket(conn => f(conn))(conn => wrapClose(conn.close()))
  }

  private[getquill] def autocommitOff(conn: Connection): (Connection, Boolean) = {
    val ac = conn.getAutoCommit
    conn.setAutoCommit(false)
    (conn, ac)
  }

  private[getquill] def autoCommitBackOn(state: (Connection, Boolean)) = {
    val (conn, wasAutocommit) = state
    wrapClose(conn.setAutoCommit(wasAutocommit))
  }

  def transaction[A](f: Task[A]): Task[A] = effect.boundary {
    Task.suspend(
      // Local read is side-effecting, need suspend
      currentConnection().map(_ => f).getOrElse {
        effect.wrap {
          val c = dataSource.getConnection()
          c.setAutoCommit(false)
          c
        }.bracket { conn =>
          TaskLocal.wrap(Task.pure(currentConnection))
            .flatMap { tl =>
              // set local for the tightest scope possible, and commit/rollback/close
              // only when nobody can touch the connection anymore
              // also, TaskLocal.bind is more hygienic then manual set/unset
              tl.bind(Some(conn))(f).guaranteeCase {
                case ExitCase.Completed => effect.wrap(conn.commit())
                case ExitCase.Error(e) => effect.wrap {
                  conn.rollback()
                  throw e
                }
                case ExitCase.Canceled => effect.wrap(conn.rollback())
              }
            }
        } { conn =>
          effect.wrapClose {
            conn.setAutoCommit(true) // Do we need this if we're closing anyway?
            conn.close()
          }
        }
      }
    ).executeWithOptions(_.enableLocalContextPropagation)
  }

  // Override with sync implementation so will actually be able to do it.
  override def probe(sql: String): Try[_] = Try {
    val c = dataSource.getConnection
    try {
      c.createStatement().execute(sql)
    } finally {
      c.close()
    }
  }

  /**
   * In order to allow a ResultSet to be consumed by an Observable, a ResultSet iterator must be created.
   * Since Quill provides a extractor for an individual ResultSet row, a single row can easily be cached
   * in memory. This allows for a straightforward implementation of a hasNext method.
   */
  class ResultSetIterator[T](rs: ResultSet, extractor: Extractor[T]) extends collection.BufferedIterator[T] {

    private final val NoData = 0
    private final val Cached = 1
    private final val Finished = 2

    private[this] var state = NoData
    private[this] var cached: T = null.asInstanceOf[T]

    protected[this] final def finished(): T = {
      state = Finished
      null.asInstanceOf[T]
    }

    /** Return a new value or call finished() */
    protected def fetchNext(): T =
      if (rs.next()) extractor(rs)
      else finished()

    def head: T = {
      prefetchIfNeeded()
      if (state == 1) cached
      else throw new NoSuchElementException("head on empty iterator")
    }

    private def prefetchIfNeeded(): Unit = {
      if (state == NoData) {
        cached = fetchNext()
        if (state == NoData) state = Cached
      }
    }

    def hasNext: Boolean = {
      prefetchIfNeeded()
      state == Cached
    }

    def next(): T = {
      prefetchIfNeeded()
      if (state == Cached) {
        state = NoData
        cached
      } else throw new NoSuchElementException("next on empty iterator")
    }
  }

  /**
   * Override to enable specific vendor options needed for streaming
   */
  protected def prepareStatementForStreaming(sql: String, conn: Connection, fetchSize: Option[Int]) = {
    val stmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
    fetchSize.foreach { size =>
      stmt.setFetchSize(size)
    }
    stmt
  }

  def streamQuery[T](fetchSize: Option[Int], sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Observable[T] =
    withConnectionObservable { conn =>
      Observable.eval {
        val stmt = prepareStatementForStreaming(sql, conn, fetchSize)
        val (params, ps) = prepare(stmt)
        logger.logQuery(sql, params)
        ps.executeQuery()
      }.bracket { rs =>
        Observable
          .fromIteratorUnsafe(new ResultSetIterator(rs, extractor))
      } { rs =>
        wrapClose(rs.close())
      }
    }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Task[Seq[String]] = {
    withConnectionWrapped { conn =>
      prepare(conn.prepareStatement(statement))._1.reverse.map(prepareParam)
    }
  }
}
