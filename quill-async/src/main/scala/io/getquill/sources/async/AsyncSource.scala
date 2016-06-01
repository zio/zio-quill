package io.getquill.sources.async

import com.github.mauricio.async.db.{ Connection, RowData }
import com.typesafe.scalalogging.Logger
import io.getquill.naming.NamingStrategy
import io.getquill.sources.BindedStatementBuilder
import io.getquill.sources.sql.idiom.SqlIdiom
import io.getquill.sources.sql.{ SqlBindedStatementBuilder, SqlSource }
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.{ global => ec }
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.Try
import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }

trait AsyncSource[D <: SqlIdiom, N <: NamingStrategy, C <: Connection]
  extends SqlSource[D, N, RowData, BindedStatementBuilder[List[Any]]]
  with Decoders
  with Encoders {
  self =>

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[AsyncSource[_, _, _]]))

  type QueryResult[T] = Future[List[T]]
  type SingleQueryResult[T] = Future[T]
  type ActionResult[T] = Future[Long]
  type BatchedActionResult[T] = Future[List[Long]]

  class ActionApply[T](f: List[T] => Future[List[Long]])(implicit ec: ExecutionContext)
    extends Function1[List[T], Future[List[Long]]] {
    def apply(params: List[T]) = f(params)

    def apply(param: T) = f(List(param)).map(_.head)
  }

  protected def connection: Connection
  protected def extractActionResult(generated: Option[String])(result: DBQueryResult): Long
  protected def expandAction(sql: String, generated: Option[String]) = sql

  def probe(sql: String) =
    Try {
      Await.result(connection.sendQuery(sql), Duration.Inf)
    }

  def transaction[T](f: AsyncSource[D, N, C] => Future[T]) = {
    connection.inTransaction { conn =>
      val transactional = new AsyncSource[D, N, C] {
        override protected def close = ()
        override protected def connection = conn
        override protected def extractActionResult(generated: Option[String])(result: DBQueryResult) =
          self.extractActionResult(generated)(result)
        override protected def expandAction(sql: String, generated: Option[String]) =
          self.expandAction(sql, generated)
      }
      f(transactional)
    }
  }

  def executeAction(sql: String, bind: BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = identity, generated: Option[String] = None)(implicit ec: ExecutionContext) = {
    logger.info(sql)
    val (expanded, params) = bind(new SqlBindedStatementBuilder).build(sql)
    connection.sendPreparedStatement(expandAction(expanded, generated), params(List())).map(extractActionResult(generated))
  }

  def executeActionBatch[T](sql: String, bindParams: T => BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = (_: T) => identity[BindedStatementBuilder[List[Any]]] _, generated: Option[String] = None)(implicit ec: ExecutionContext): ActionApply[T] = {
    def run(values: List[T]): Future[List[Long]] =
      values match {
        case Nil =>
          Future.successful(List())
        case value :: tail =>
          val (expanded, params) = bindParams(value)(new SqlBindedStatementBuilder).build(sql)
          logger.info(expanded.toString)
          connection.sendPreparedStatement(expandAction(expanded, generated), params(List())).map(extractActionResult(generated))
            .flatMap(r => run(tail).map(r +: _))
      }
    new ActionApply(run _)
  }

  def executeQuery[T](sql: String, extractor: RowData => T = identity[RowData] _, bind: BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = identity)(implicit ec: ExecutionContext) = {
    val (expanded, params) = bind(new SqlBindedStatementBuilder).build(sql)
    logger.info(expanded.toString)
    connection.sendPreparedStatement(expanded, params(List())).map {
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
