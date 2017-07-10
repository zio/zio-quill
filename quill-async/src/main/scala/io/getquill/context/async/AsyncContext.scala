package io.getquill.context.async

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.github.mauricio.async.db.RowData
import com.github.mauricio.async.db.pool.PartitionedConnectionPool
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Try
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.NamingStrategy
import io.getquill.util.ContextLogger

abstract class AsyncContext[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](pool: PartitionedConnectionPool[C])
  extends SqlContext[D, N]
  with Decoders
  with Encoders {

  private val logger = ContextLogger(classOf[AsyncContext[_, _, _]])

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

  protected def withConnection[T](f: Connection => Future[T])(implicit ec: ExecutionContext) =
    ec match {
      case TransactionalExecutionContext(ec, conn) => f(conn)
      case other                                   => f(pool)
    }

  protected def extractActionResult[O](returningColumn: String, extractor: Extractor[O])(result: DBQueryResult): O

  protected def expandAction(sql: String, returningColumn: String) = sql

  def probe(sql: String) =
    Try {
      Await.result(pool.sendQuery(sql), Duration.Inf)
    }

  def transaction[T](f: TransactionalExecutionContext => Future[T])(implicit ec: ExecutionContext) =
    pool.inTransaction { c =>
      f(TransactionalExecutionContext(ec, c))
    }

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext): Future[List[T]] = {
    val (params, values) = prepare(Nil)
    logger.logQuery(sql, params)
    withConnection(_.sendPreparedStatement(sql, values)).map {
      _.rows match {
        case Some(rows) => rows.map(extractor).toList
        case None       => Nil
      }
    }
  }

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext): Future[T] =
    executeQuery(sql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare)(implicit ec: ExecutionContext): Future[Long] = {
    val (params, values) = prepare(Nil)
    logger.logQuery(sql, params)
    withConnection(_.sendPreparedStatement(sql, values)).map(_.rowsAffected)
  }

  def executeActionReturning[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T], returningColumn: String)(implicit ec: ExecutionContext): Future[T] = {
    val expanded = expandAction(sql, returningColumn)
    val (params, values) = prepare(Nil)
    logger.logQuery(sql, params)
    withConnection(_.sendPreparedStatement(expanded, values))
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

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(implicit ec: ExecutionContext): Future[List[T]] =
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
