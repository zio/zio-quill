package io.getquill.context.qzio

import io.getquill.context.ZioJdbc._
import io.getquill.context.jdbc.JdbcContextVerbExecute
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.{ ContextVerbStream, ExecutionInfo }
import io.getquill.util.ContextLogger
import io.getquill.{ NamingStrategy, ReturnAction }
import zio.Exit.{ Failure, Success }
import zio.ZIO.blocking
import zio.stream.ZStream
import zio.{ Cause, StackTrace, ZIO }

import java.sql.{ Array => _, _ }
import javax.sql.DataSource
import scala.reflect.ClassTag
import scala.util.Try

abstract class ZioJdbcUnderlyingContext[Dialect <: SqlIdiom, Naming <: NamingStrategy] extends ZioContext[Dialect, Naming]
  with JdbcContextVerbExecute[Dialect, Naming]
  with ContextVerbStream[Dialect, Naming]
  with ZioPrepareContext[Dialect, Naming]
  with ZioTranslateContext {

  override private[getquill] val logger = ContextLogger(classOf[ZioJdbcUnderlyingContext[_, _]])

  override type Error = SQLException
  override type Environment = Session
  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  // Need explicit return-type annotations due to scala/bug#8356. Otherwise macro system will not understand Result[Long]=Task[Long] etc...
  override def executeAction(sql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner): QCIO[Long] =
    super.executeAction(sql, prepare)(info, dc)
  override def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner): QCIO[List[T]] =
    super.executeQuery(sql, prepare, extractor)(info, dc)
  override def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner): QCIO[T] =
    super.executeQuerySingle(sql, prepare, extractor)(info, dc)
  override def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction)(info: ExecutionInfo, dc: Runner): QCIO[O] =
    super.executeActionReturning(sql, prepare, extractor, returningBehavior)(info, dc)
  override def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner): QCIO[List[Long]] =
    super.executeBatchAction(groups)(info, dc)
  override def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(info: ExecutionInfo, dc: Runner): QCIO[List[T]] =
    super.executeBatchActionReturning(groups, extractor)(info, dc)
  override def prepareQuery(sql: String, prepare: Prepare)(info: ExecutionInfo, dc: Runner): QCIO[PreparedStatement] =
    super.prepareQuery(sql, prepare)(info, dc)
  override def prepareAction(sql: String, prepare: Prepare)(info: ExecutionInfo, dc: Runner): QCIO[PreparedStatement] =
    super.prepareAction(sql, prepare)(info, dc)
  override def prepareBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner): QCIO[List[PreparedStatement]] =
    super.prepareBatchAction(groups)(info, dc)

  /** ZIO Contexts do not managed DB connections so this is a no-op */
  override def close(): Unit = ()

  protected def withConnection[T](f: Connection => Result[T]): Result[T] = throw new IllegalArgumentException("Not Used")

  // Primary method used to actually run Quill context commands query, insert, update, delete and others
  override protected def withConnectionWrapped[T](f: Connection => T): QCIO[T] =
    blocking {
      for {
        conn <- ZIO.service[Connection]
        result <- sqlEffect(f(conn))
      } yield result
    }

  private def sqlEffect[T](t: => T): QCIO[T] = ZIO.attempt(t).refineToOrDie[SQLException]

  /**
   * Note that for ZIO 2.0 since the env is covariant, R can be a subtype of connection because if there are other with-clauses
   * they can be generalized to Something <: Connection. E.g. `Connection with OtherStuff` generalizes to `Something <: Connection`.
   */
  private[getquill] def withoutAutoCommit[R <: Connection, A, E <: Throwable: ClassTag](f: ZIO[R, E, A]): ZIO[R, E, A] = {
    for {
      conn <- ZIO.service[Connection]
      autoCommitPrev = conn.getAutoCommit
      r <- ZIO.acquireReleaseWith(sqlEffect(conn))(conn => ZIO.succeed(conn.setAutoCommit(autoCommitPrev))) { conn =>
        sqlEffect(conn.setAutoCommit(false)).flatMap(_ => f)
      }.refineToOrDie[E]
    } yield r
  }

  private[getquill] def streamWithoutAutoCommit[A](f: ZStream[Connection, Throwable, A]): ZStream[Connection, Throwable, A] = {
    for {
      conn <- ZStream.service[Connection]
      autoCommitPrev = conn.getAutoCommit
      r <- ZStream.acquireReleaseWith(ZIO.attempt(conn.setAutoCommit(false)))(_ => {
        ZIO.succeed(conn.setAutoCommit(autoCommitPrev))
      }).flatMap(_ => f)
    } yield r
  }

  def transaction[R <: Connection, A](f: ZIO[R, Throwable, A]): ZIO[R, Throwable, A] = {
    ZIO.environment[R].flatMap(env =>
      blocking(withoutAutoCommit(
        f.onExit {
          case Success(_) =>
            ZIO.succeed(env.get[Connection].commit())
          case Failure(cause) =>
            ZIO.succeed(env.get[Connection].rollback()).foldCauseZIO(
              // NOTE: cause.flatMap(Cause.die) means wrap up the throwable failures into die failures, can only do if E param is Throwable (can also do .orDie at the end)
              rollbackFailCause => ZIO.failCause(cause.flatMap(Cause.die(_, StackTrace.none)) ++ rollbackFailCause),
              _ => ZIO.failCause(cause.flatMap(Cause.die(_, StackTrace.none))) // or ZIO.halt(cause).orDie
            )
        }.provideEnvironment(env)
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

  def streamQuery[T](fetchSize: Option[Int], sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner): QCStream[T] = {
    def prepareStatement(conn: Connection) = {
      val stmt = prepareStatementForStreaming(sql, conn, fetchSize)
      val (params, ps) = prepare(stmt, conn)
      logger.logQuery(sql, params)
      ps
    }

    val managedEnv: ZStream[Connection, Throwable, (Connection, PrepareRow, ResultSet)] =
      ZStream.scoped {
        for {
          conn <- ZIO.service[Connection]
          ps <- scopedBestEffort(ZIO.attempt(prepareStatement(conn)))
          rs <- scopedBestEffort(ZIO.attempt(ps.executeQuery()))
        } yield (conn, ps, rs)
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
              ZStream.fromIterator(new ResultSetIterator(rs, conn, extractor))
          }
      }

    // Run the chunked fetch on the blocking pool
    streamBlocker *> streamWithoutAutoCommit(outStream).refineToOrDie[SQLException]
  }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): QCIO[Seq[String]] = {
    withConnectionWrapped { conn =>
      prepare(conn.prepareStatement(statement), conn)._1.reverse.map(prepareParam)
    }
  }

  // Generally these are not used in the ZIO context but have implementations in case they are needed
  override def wrap[T](t: => T): ZIO[Connection, SQLException, T] = QCIO(t)
  override def push[A, B](result: ZIO[Connection, SQLException, A])(f: A => B): ZIO[Connection, SQLException, B] = result.map(f)
  override def seq[A](f: List[ZIO[Connection, SQLException, A]]): ZIO[Connection, SQLException, List[A]] = ZIO.collectAll(f)
}
