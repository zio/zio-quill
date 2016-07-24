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

import io.getquill.context.BindedStatementBuilder
import io.getquill.context.sql.SqlBindedStatementBuilder
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.NamingStrategy

abstract class AsyncContext[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](pool: PartitionedConnectionPool[C])
  extends SqlContext[D, N, RowData, BindedStatementBuilder[List[Any]]]
  with Decoders
  with Encoders {

  private val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[AsyncContext[_, _, _]]))

  type QueryResult[T] = Future[List[T]]
  type SingleQueryResult[T] = Future[T]
  type ActionResult[T, O] = Future[O]
  type BatchedActionResult[T, O] = Future[List[O]]

  override def close = {
    Await.result(pool.close, Duration.Inf)
    ()
  }

  private def withConnection[T](f: Connection => Future[T])(implicit ec: ExecutionContext) =
    ec match {
      case TransactionalExecutionContext(ec, conn) => f(conn)
      case other                                   => f(pool)
    }

  protected def extractActionResult[O](generated: Option[String], returningExtractor: RowData => O)(result: DBQueryResult): O

  protected def expandAction(sql: String, generated: Option[String]) = sql

  def probe(sql: String) =
    Try {
      Await.result(pool.sendQuery(sql), Duration.Inf)
    }

  def transaction[T](f: TransactionalExecutionContext => Future[T])(implicit ec: ExecutionContext) =
    pool.inTransaction { c =>
      f(TransactionalExecutionContext(ec, c))
    }

  def executeAction[O](
    sql:                String,
    bind:               BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = identity,
    generated:          Option[String]                                                         = None,
    returningExtractor: RowData => O                                                           = identity[RowData] _
  )(implicit ec: ExecutionContext) = {
    logger.info(sql)
    val (expanded, params) = bind(new SqlBindedStatementBuilder).build(sql)
    withConnection(_.sendPreparedStatement(expandAction(expanded, generated), params(List()))).map(extractActionResult(generated, returningExtractor)(_))
  }

  def executeActionBatch[T, O](
    sql:                String,
    bindParams:         T => BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = (_: T) => identity[BindedStatementBuilder[List[Any]]] _,
    generated:          Option[String]                                                              = None,
    returningExtractor: RowData => O                                                                = identity[RowData] _
  )(implicit ec: ExecutionContext): List[T] => Future[List[O]] = {
    def run(values: List[T]): Future[List[O]] =
      values match {
        case Nil =>
          Future.successful(List())
        case value :: tail =>
          val (expanded, params) = bindParams(value)(new SqlBindedStatementBuilder).build(sql)
          logger.info(expanded.toString)
          withConnection(conn => conn.sendPreparedStatement(expandAction(expanded, generated), params(List())).map(extractActionResult(generated, returningExtractor)(_)))
            .flatMap(r => run(tail).map(r +: _))
      }
    run _
  }

  def executeQuery[T](sql: String, extractor: RowData => T = identity[RowData] _, bind: BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = identity)(implicit ec: ExecutionContext) = {
    val (expanded, params) = bind(new SqlBindedStatementBuilder).build(sql)
    logger.info(expanded.toString)
    withConnection(_.sendPreparedStatement(expanded, params(List()))).map {
      _.rows match {
        case Some(rows) => rows.map(extractor).toList
        case None       => List()
      }
    }
  }

  def executeQuerySingle[T](sql: String, extractor: RowData => T = identity[RowData] _, bind: BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = identity)(implicit ec: ExecutionContext) = {
    executeQuery(sql, extractor, bind).map(handleSingleResult)
  }

}
