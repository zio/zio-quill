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

abstract class AsyncContext[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](pool: PartitionedConnectionPool[C])
  extends SqlContext[D, N]
  with Decoders
  with Encoders {

  private val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[AsyncContext[_, _, _]]))

  override type PrepareRow = List[Any]
  override type ResultRow = RowData

  override type RunQueryResult[T] = Future[List[T]]
  override type RunQuerySingleResult[T] = Future[T]
  override type RunActionResult = Future[Long]
  override type RunActionReturningResult[T] = Future[T]
  override type RunBatchActionResult = Future[List[Long]]
  override type RunBatchActionReturningResult[T] = Future[List[T]]

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

  def executeQuery[T](sql: String, prepare: List[Any] => List[Any] = identity, extractor: RowData => T = identity[RowData] _)(implicit ec: ExecutionContext): Future[List[T]] = {
    logger.debug(sql)
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
    logger.debug(sql)
    withConnection(_.sendPreparedStatement(sql, prepare(List()))).map(_.rowsAffected)
  }

  def executeActionReturning[T](sql: String, prepare: List[Any] => List[Any] = identity, extractor: RowData => T, returningColumn: String)(implicit ec: ExecutionContext): Future[T] = {
    val expanded = expandAction(sql, returningColumn)
    logger.debug(expanded)
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
