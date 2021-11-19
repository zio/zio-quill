package io.getquill.context.qzio

import io.getquill.context.ZioJdbc._
import io.getquill.context.jdbc.JdbcRunContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.{ ContextEffect, ExecutionInfo, StreamingContext }
import io.getquill.util.ContextLogger
import io.getquill.{ NamingStrategy, ReturnAction }
import zio.Exit.{ Failure, Success }
import zio.stream.{ Stream, ZStream }
import zio.{ Cause, Has, Task, UIO, ZIO, ZManaged }

import java.sql.{ Array => _, _ }
import javax.sql.DataSource
import scala.reflect.ClassTag
import scala.util.Try

abstract class ZioJdbcUnderlyingContext[Dialect <: SqlIdiom, Naming <: NamingStrategy] extends ZioContext[Dialect, Naming]
  with JdbcRunContext[Dialect, Naming]
  with StreamingContext[Dialect, Naming]
  with ZioPrepareContext[Dialect, Naming]
  with ZioTranslateContext {

  override private[getquill] val logger = ContextLogger(classOf[ZioJdbcUnderlyingContext[_, _]])

  override type Error = SQLException
  override type Environment = Has[Session]
  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  // Need explicit return-type annotations due to scala/bug#8356. Otherwise macro system will not understand Result[Long]=Task[Long] etc...
  override def executeAction[T](sql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: DatasourceContext): QCIO[Long] =
    super.executeAction(sql, prepare)(info, dc)
  override def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): QCIO[List[T]] =
    super.executeQuery(sql, prepare, extractor)(info, dc)
  override def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): QCIO[T] =
    super.executeQuerySingle(sql, prepare, extractor)(info, dc)
  override def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction)(info: ExecutionInfo, dc: DatasourceContext): QCIO[O] =
    super.executeActionReturning(sql, prepare, extractor, returningBehavior)(info, dc)
  override def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext): QCIO[List[Long]] =
    super.executeBatchAction(groups)(info, dc)
  override def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(info: ExecutionInfo, dc: DatasourceContext): QCIO[List[T]] =
    super.executeBatchActionReturning(groups, extractor)(info, dc)
  override def prepareQuery(sql: String, prepare: Prepare)(info: ExecutionInfo, dc: DatasourceContext): QCIO[PreparedStatement] =
    super.prepareQuery(sql, prepare)(info, dc)
  override def prepareAction(sql: String, prepare: Prepare)(info: ExecutionInfo, dc: DatasourceContext): QCIO[PreparedStatement] =
    super.prepareAction(sql, prepare)(info, dc)
  override def prepareBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext): QCIO[List[PreparedStatement]] =
    super.prepareBatchAction(groups)(info, dc)

  /** ZIO Contexts do not managed DB connections so this is a no-op */
  override def close(): Unit = ()

  protected def withConnection[T](f: Connection => Result[T]): Result[T] = throw new IllegalArgumentException("Not Used")

  // Primary method used to actually run Quill context commands query, insert, update, delete and others
  override protected def withConnectionWrapped[T](f: Connection => T): QCIO[T] =
    withBlocking {
      for {
        conn <- ZIO.environment[Has[Connection]]
        result <- sqlEffect(f(conn.get[Connection]))
      } yield result
    }

  private def sqlEffect[T](t: => T): QCIO[T] = ZIO.effect(t).refineToOrDie[SQLException]

  private[getquill] def withoutAutoCommit[R <: Has[Connection], A, E <: Throwable: ClassTag](f: ZIO[R, E, A]): ZIO[R, E, A] = {
    for {
      blockingConn <- ZIO.environment[Has[Connection]]
      conn = blockingConn.get[Connection]
      autoCommitPrev = conn.getAutoCommit
      r <- sqlEffect(conn).bracket(conn => UIO(conn.setAutoCommit(autoCommitPrev))) { conn =>
        sqlEffect(conn.setAutoCommit(false)).flatMap(_ => f)
      }.refineToOrDie[E]
    } yield r
  }

  private[getquill] def streamWithoutAutoCommit[A](f: ZStream[Has[Connection], Throwable, A]): ZStream[Has[Connection], Throwable, A] = {
    for {
      blockingConn <- ZStream.environment[Has[Connection]]
      conn = blockingConn.get[Connection]
      autoCommitPrev = conn.getAutoCommit
      r <- ZStream.bracket(Task(conn.setAutoCommit(false)))(_ => UIO(conn.setAutoCommit(autoCommitPrev))).flatMap(_ => f)
    } yield r
  }

  def transaction[R <: Has[Connection], A](f: ZIO[R, Throwable, A]): ZIO[R, Throwable, A] = {
    ZIO.environment[R].flatMap(env =>
      withBlocking(withoutAutoCommit(
        f.onExit {
          case Success(_) =>
            UIO(env.get[Connection].commit())
          case Failure(cause) =>
            UIO(env.get[Connection].rollback()).foldCauseM(
              // NOTE: cause.flatMap(Cause.die) means wrap up the throwable failures into die failures, can only do if E param is Throwable (can also do .orDie at the end)
              rollbackFailCause => ZIO.halt(cause.flatMap(Cause.die) ++ rollbackFailCause),
              _ => ZIO.halt(cause.flatMap(Cause.die)) // or ZIO.halt(cause).orDie
            )
        }.provide(env)
      )))
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
   * Override to enable specific vendor options needed for streaming
   */
  protected def prepareStatementForStreaming(sql: String, conn: Connection, fetchSize: Option[Int]) = {
    val stmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
    fetchSize.foreach { size =>
      stmt.setFetchSize(size)
    }
    stmt
  }

  def streamQuery[T](fetchSize: Option[Int], sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): QCStream[T] = {
    def prepareStatement(conn: Connection) = {
      val stmt = prepareStatementForStreaming(sql, conn, fetchSize)
      val (params, ps) = prepare(stmt, conn)
      logger.logQuery(sql, params)
      ps
    }

    val managedEnv: ZStream[Connection, Throwable, (Connection, PrepareRow, ResultSet)] =
      ZStream.environment[Connection].flatMap { conn =>
        ZStream.managed {
          for {
            conn <- ZManaged.make(Task(conn))(c => Task.unit)
            ps <- managedBestEffort(Task(prepareStatement(conn)))
            rs <- managedBestEffort(Task(ps.executeQuery()))
          } yield (conn, ps, rs)
        }
      }

    val outStream: ZStream[Connection, Throwable, T] =
      managedEnv.flatMap {
        case (conn, ps, rs) =>
          val iter = new ResultSetIterator(rs, conn, extractor)
          fetchSize match {
            // TODO Assuming chunk size is fetch size. Not sure if this is optimal.
            //      Maybe introduce some switches to control this?
            case Some(size) =>
              ZStream.fromIterator(iter, size)
            case None =>
              Stream.fromIterator(new ResultSetIterator(rs, conn, extractor))
          }
      }

    val typedStream = outStream.provideSome((bc: Has[Connection]) => bc.get[Connection])
    // Run the chunked fetch on the blocking pool
    streamBlocker *> streamWithoutAutoCommit(typedStream).refineToOrDie[SQLException]
  }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): QCIO[Seq[String]] = {
    withConnectionWrapped { conn =>
      prepare(conn.prepareStatement(statement), conn)._1.reverse.map(prepareParam)
    }
  }

  // Put this last since we want to be able to use zio 'effect' keyword in some places
  override protected val effect = new ContextEffect[Result] {
    override def wrap[T](t: => T): ZIO[Has[Connection], SQLException, T] =
      throw new IllegalArgumentException("Runner not used for zio context.")
    override def push[A, B](result: ZIO[Has[Connection], SQLException, A])(f: A => B): ZIO[Has[Connection], SQLException, B] =
      throw new IllegalArgumentException("Runner not used for zio context.")
    override def seq[A](f: List[ZIO[Has[Connection], SQLException, A]]): ZIO[Has[Connection], SQLException, List[A]] =
      throw new IllegalArgumentException("Runner not used for zio context.")
  }
}
