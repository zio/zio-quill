package io.getquill.context.qzio

import io.getquill.context.{ ContextEffect, StreamingContext }
import io.getquill.context.ZioJdbc._
import io.getquill.context.jdbc.JdbcRunContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.ContextLogger
import io.getquill.{ NamingStrategy, ReturnAction }
import zio.Exit.{ Failure, Success }
import zio.stream.{ Stream, ZStream }
import zio.{ Cause, Chunk, ChunkBuilder, Has, Task, UIO, ZIO, ZManaged }

import java.sql.{ Array => _, _ }
import javax.sql.DataSource
import scala.util.Try
import zio.blocking.{ Blocking, blocking }

import scala.reflect.ClassTag

/**
 * Quill context that executes JDBC queries inside of ZIO. Unlike most other contexts
 * that require passing in a Data Source, this context takes in a java.sql.Connection
 * as a resource dependency which can be provided later (see `ZioJdbc` for helper methods
 * that assist in doing this).
 *
 * The resource dependency itself is not just a Connection since JDBC requires blocking.
 * Instead it is a `Has[Connection] with Has[Blocking.Service]` which is type-alised as
 * `QConnection` hence methods in this context return `ZIO[QConnection, Throwable, T]`.
 * The type `QIO[T]` i.e. Quill-IO is an alias for this.
 *
 * If you have a zio-app, using this context is fairly straightforward but requires some setup:
 * {{
 *   val zioConn =
 *     ZLayer.fromManaged(for {
 *       ds <- ZManaged.fromAutoCloseable(Task(JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource))
 *       conn <- ZManaged.fromAutoCloseable(Task(ds.getConnection))
 *     } yield conn)
 *
 *   MyZioContext.run(query[Person]).provideCustomLayer(zioConn)
 * }}
 *
 * Various methods in the `io.getquill.context.ZioJdbc` can assist in simplifying it's creation, for example, you can
 * provide a `DataSource` instead of a `Connection` like this
 * (note that the resulting Connection has a closing bracket).
 * {{
 *   import ZioJdbc._
 *   val zioConn = QDataSource.fromPrefix("testPostgresDB") >>> QDataSource.toConnection
 *   MyZioContext.run(query[Person]).provideCustomLayer(zioConn)
 * }}
 *
 * If you are using a Plain Scala app however, you will need to manually run it e.g. using zio.Runtime
 * {{
 *   Runtime.default.unsafeRun(MyZioContext.run(query[Person]).provideCustomLayer(zioConn))
 * }}
 */
abstract class ZioJdbcContext[Dialect <: SqlIdiom, Naming <: NamingStrategy] extends ZioContext[Dialect, Naming]
  with JdbcRunContext[Dialect, Naming]
  with StreamingContext[Dialect, Naming]
  with ZioPrepareContext[Dialect, Naming]
  with ZioTranslateContext {

  override private[getquill] val logger = ContextLogger(classOf[ZioJdbcContext[_, _]])

  override type Error = SQLException
  override type Environment = Has[Session] with Blocking
  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  // Need explicit return-type annotations due to scala/bug#8356. Otherwise macro system will not understand Result[Long]=Task[Long] etc...
  override def executeAction[T](sql: String, prepare: Prepare = identityPrepare): QIO[Long] =
    super.executeAction(sql, prepare)
  override def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): QIO[List[T]] =
    super.executeQuery(sql, prepare, extractor)
  override def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): QIO[T] =
    super.executeQuerySingle(sql, prepare, extractor)
  override def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction): QIO[O] =
    super.executeActionReturning(sql, prepare, extractor, returningBehavior)
  override def executeBatchAction(groups: List[BatchGroup]): QIO[List[Long]] =
    super.executeBatchAction(groups)
  override def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): QIO[List[T]] =
    super.executeBatchActionReturning(groups, extractor)
  override def prepareQuery[T](sql: String, prepare: Prepare, extractor: Extractor[T] = identityExtractor): QIO[PreparedStatement] =
    super.prepareQuery(sql, prepare, extractor)
  override def prepareAction(sql: String, prepare: Prepare): QIO[PreparedStatement] =
    super.prepareAction(sql, prepare)
  override def prepareBatchAction(groups: List[BatchGroup]): QIO[List[PreparedStatement]] =
    super.prepareBatchAction(groups)

  /** ZIO Contexts do not managed DB connections so this is a no-op */
  override def close(): Unit = ()

  protected def withConnection[T](f: Connection => Result[T]): Result[T] = throw new IllegalArgumentException("Not Used")

  // Primary method used to actually run Quill context commands query, insert, update, delete and others
  override protected def withConnectionWrapped[T](f: Connection => T): QIO[T] =
    blocking {
      for {
        conn <- ZIO.environment[QConnection]
        result <- sqlEffect(f(conn.get[Connection]))
      } yield result
    }

  private def sqlEffect[T](t: => T): QIO[T] = ZIO.effect(t).refineToOrDie[SQLException]

  private[getquill] def withoutAutoCommit[A, E <: Throwable: ClassTag](f: ZIO[QConnection, E, A]): ZIO[QConnection, E, A] = {
    for {
      blockingConn <- ZIO.environment[QConnection]
      conn = blockingConn.get[Connection]
      autoCommitPrev = conn.getAutoCommit
      r <- sqlEffect(conn).bracket(conn => UIO(conn.setAutoCommit(autoCommitPrev))) { conn =>
        sqlEffect(conn.setAutoCommit(false)).flatMap(_ => f)
      }.refineToOrDie[E]
    } yield r
  }

  private[getquill] def streamWithoutAutoCommit[A](f: ZStream[QConnection, Throwable, A]): ZStream[QConnection, Throwable, A] = {
    for {
      blockingConn <- ZStream.environment[QConnection]
      conn = blockingConn.get[Connection]
      autoCommitPrev = conn.getAutoCommit
      r <- ZStream.bracket(Task(conn.setAutoCommit(false)))(_ => UIO(conn.setAutoCommit(autoCommitPrev))).flatMap(_ => f)
    } yield r
  }

  def transaction[A](f: ZIO[QConnection, Throwable, A]): ZIO[QConnection, Throwable, A] = {
    blocking(withoutAutoCommit(ZIO.environment[QConnection].flatMap(conn =>
      f.onExit {
        case Success(_) =>
          UIO(conn.get[Connection].commit())
        case Failure(cause) =>
          UIO(conn.get[Connection].rollback()).foldCauseM(
            // NOTE: cause.flatMap(Cause.die) means wrap up the throwable failures into die failures, can only do if E param is Throwable (can also do .orDie at the end)
            rollbackFailCause => ZIO.halt(cause.flatMap(Cause.die) ++ rollbackFailCause),
            _ => ZIO.halt(cause.flatMap(Cause.die)) // or ZIO.halt(cause).orDie
          )
      })))
  }

  def probingDataSource: Option[DataSource] = None

  // Override with sync implementation so will actually be able to do it.
  override def probe(sql: String): Try[_] =
    probingDataSource match {
      case Some(dataSource) =>
        Try {
          val c = dataSource.getConnection
          try {
            c.createStatement().execute(sql)
          } finally {
            c.close()
          }
        }
      case None => Try[Unit](())
    }

  /**
   * In order to allow a ResultSet to be consumed by an Observable, a ResultSet iterator must be created.
   * Since Quill provides a extractor for an individual ResultSet row, a single row can easily be cached
   * in memory. This allows for a straightforward implementation of a hasNext method.
   */
  class ResultSetIterator[T](rs: ResultSet, extractor: Extractor[T]) extends BufferedIterator[T] {

    private[this] var state = 0 // 0: no data, 1: cached, 2: finished
    private[this] var cached: T = null.asInstanceOf[T]

    protected[this] final def finished(): T = {
      state = 2
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
      if (state == 0) {
        cached = fetchNext()
        if (state == 0) state = 1
      }
    }

    def hasNext: Boolean = {
      prefetchIfNeeded()
      state == 1
    }

    def next(): T = {
      prefetchIfNeeded()
      if (state == 1) {
        state = 0
        cached
      } else throw new NoSuchElementException("next on empty iterator");
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

  def streamQuery[T](fetchSize: Option[Int], sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): QStream[T] = {
    def prepareStatement(conn: Connection) = {
      val stmt = prepareStatementForStreaming(sql, conn, fetchSize)
      val (params, ps) = prepare(stmt)
      logger.logQuery(sql, params)
      ps
    }

    val managedEnv: ZStream[Connection, Throwable, (Connection, PrepareRow, ResultSet)] =
      ZStream.environment[Connection].flatMap { conn =>
        ZStream.managed {
          for {
            conn <- ZManaged.make(Task(conn))(c => Task.unit)
            ps <- ZManaged.fromAutoCloseable(Task(prepareStatement(conn)))
            rs <- ZManaged.fromAutoCloseable(Task(ps.executeQuery()))
          } yield (conn, ps, rs)
        }
      }

    val outStream: ZStream[Connection, Throwable, T] =
      managedEnv.flatMap {
        case (conn, ps, rs) =>
          val iter = new ResultSetIterator(rs, extractor)
          fetchSize match {
            // TODO Assuming chunk size is fetch size. Not sure if this is optimal.
            //      Maybe introduce some switches to control this?
            case Some(size) =>
              chunkedFetch(iter, size)
            case None =>
              Stream.fromIterator(new ResultSetIterator(rs, extractor))
          }
      }

    val typedStream = outStream.provideSome((bc: QConnection) => bc.get[Connection])
    // Run the chunked fetch on the blocking pool
    streamBlocker *> streamWithoutAutoCommit(typedStream).refineToOrDie[SQLException]
  }

  val streamBlocker: ZStream[Blocking, Nothing, Any] =
    ZStream.managed(zio.blocking.blockingExecutor.toManaged_.flatMap { executor =>
      ZManaged.lock(executor)
    })

  def guardedChunkFill[A](n: Int)(hasNext: => Boolean, elem: => A): Chunk[A] =
    if (n <= 0) Chunk.empty
    else {
      val builder = ChunkBuilder.make[A]()
      builder.sizeHint(n)

      var i = 0
      while (i < n && hasNext) {
        builder += elem
        i += 1
      }
      builder.result()
    }

  def chunkedFetch[T](iter: ResultSetIterator[T], fetchSize: Int) = {
    object StreamEnd extends Throwable
    ZStream.fromEffect(Task(iter) <*> ZIO.runtime[Any]).flatMap {
      case (it, rt) =>
        ZStream.repeatEffectChunkOption {
          Task {
            val hasNext: Boolean =
              try it.hasNext
              catch {
                case e: Throwable if !rt.platform.fatal(e) =>
                  throw e
              }
            if (hasNext) {
              try {
                // The most efficent way to load an array is to allocate a slice that has the number of elements
                // that will be returned by every database fetch i.e. the fetch size. Since the later iteration
                // may return fewer elements then that, we need a special guard for that particular scenario.
                // However, since we do not know which slice is that last, the guard (i.e. hasNext())
                // needs to be used for all of them.
                guardedChunkFill(fetchSize)(it.hasNext, it.next())
              } catch {
                case e: Throwable if !rt.platform.fatal(e) =>
                  throw e
              }
            } else throw StreamEnd
          }.mapError {
            case StreamEnd => None
            case e         => Some(e)
          }
        }
    }
  }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): QIO[Seq[String]] = {
    withConnectionWrapped { conn =>
      prepare(conn.prepareStatement(statement))._1.reverse.map(prepareParam)
    }
  }

  // Put this last since we want to be able to use zio 'effect' keyword in some places
  override protected val effect = new ContextEffect[Result] {
    override def wrap[T](t: => T): ZIO[Has[Connection] with Blocking, SQLException, T] =
      throw new IllegalArgumentException("Runner not used for zio context.")
    override def push[A, B](result: ZIO[Has[Connection] with Blocking, SQLException, A])(f: A => B): ZIO[Has[Connection] with Blocking, SQLException, B] =
      throw new IllegalArgumentException("Runner not used for zio context.")
    override def seq[A](f: List[ZIO[Has[Connection] with Blocking, SQLException, A]]): ZIO[Has[Connection] with Blocking, SQLException, List[A]] =
      throw new IllegalArgumentException("Runner not used for zio context.")
  }
}
