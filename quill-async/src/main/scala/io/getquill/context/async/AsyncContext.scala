package io.getquill.context.async

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.github.mauricio.async.db.RowData
import com.github.mauricio.async.db.pool.PartitionedConnectionPool
import com.typesafe.scalalogging.Logger

import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Try

import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.NamingStrategy
import io.getquill.monad.ScalaFutureIOMonad

abstract class AsyncContext[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](pool: PartitionedConnectionPool[C])
  extends SqlContext[D, N]
  with Decoders
  with Encoders
  with ScalaFutureIOMonad {

  private val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[AsyncContext[_, _, _]]))

  override type PrepareRow = List[Any]
  override type ResultRow = RowData

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  override def close = {
    Await.result(pool.close, Duration.Inf)
    ()
  }

  private def withConnection[T](f: Connection => Future[T])(implicit ec: ExecutionContext) =
    ec match {
      case TransactionalExecutionContext(ec, conn) => f(conn)
      case other                                   => f(pool)
    }

  protected def extractActionResult[O](returningColumn: String, extractor: RowData => O)(result: DBQueryResult): O

  protected def expandAction(sql: String, returningColumn: String) = sql

  def probe(sql: String) =
    Try {
      Await.result(pool.sendQuery(sql), Duration.Inf)
    }

  def transaction[T](f: TransactionalExecutionContext => Future[T])(implicit ec: ExecutionContext) =
    pool.inTransaction { c =>
      f(TransactionalExecutionContext(ec, c))
    }

  override def unsafePerformIO[T](io: IO[T, _], transactional: Boolean = false)(implicit ec: ExecutionContext): Result[T] =
    transactional match {
      case false => super.unsafePerformIO(io)
      case true  => transaction(super.unsafePerformIO(io)(_))
    }

  def executeQuery[T](sql: String, prepare: List[Any] => List[Any] = identity, extractor: RowData => T = identity[RowData] _)(implicit ec: ExecutionContext): Future[List[T]] = {
    logger.info(sql)
    withConnection(_.sendPreparedStatement(sql, prepare(List()))).map {
      _.rows match {
        case Some(rows) => rows.map(extractor).toList
        case None       => List()
      }
    }
  }

  def executeQuerySingle[T](sql: String, prepare: List[Any] => List[Any] = identity, extractor: RowData => T = identity[RowData] _)(implicit ec: ExecutionContext): Future[T] =
    executeQuery(sql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](sql: String, prepare: List[Any] => List[Any] = identity)(implicit ec: ExecutionContext): Future[Long] = {
    logger.info(sql)
    withConnection(_.sendPreparedStatement(sql, prepare(List()))).map(_.rowsAffected)
  }

  def executeActionReturning[T](sql: String, prepare: List[Any] => List[Any] = identity, extractor: RowData => T, returningColumn: String)(implicit ec: ExecutionContext): Future[T] = {
    val expanded = expandAction(sql, returningColumn)
    logger.info(expanded)
    withConnection(_.sendPreparedStatement(expanded, prepare(List())))
      .map(extractActionResult(returningColumn, extractor))
  }

  def executeBatchAction(groups: List[BatchGroup])(implicit ec: ExecutionContext): Future[List[Long]] =
    Future.sequence {
      groups.map {
        case BatchGroup(sql, prepare) =>
          prepare.foldLeft(Future.successful(List.empty[Long])) {
            case (acc, prepare) =>
              acc.flatMap { list =>
                executeAction(sql, prepare).map(list :+ _)
              }
          }
      }
    }.map(_.flatten.toList)

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: RowData => T)(implicit ec: ExecutionContext): Future[List[T]] =
    Future.sequence {
      groups.map {
        case BatchGroupReturning(sql, column, prepare) =>
          prepare.foldLeft(Future.successful(List.empty[T])) {
            case (acc, prepare) =>
              acc.flatMap { list =>
                executeActionReturning(sql, prepare, extractor, column).map(list :+ _)
              }
          }
      }
    }.map(_.flatten.toList)
}
