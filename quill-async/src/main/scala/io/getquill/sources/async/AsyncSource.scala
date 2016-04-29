package io.getquill.sources.async

import com.github.mauricio.async.db.{ Connection, RowData }
import com.typesafe.scalalogging.Logger
import io.getquill.naming.NamingStrategy
import io.getquill.sources.BindedStatementBuilder
import io.getquill.sources.sql.idiom.SqlIdiom
import io.getquill.sources.sql.{SqlBindedStatementBuilder, SqlSource}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.experimental.macros
import scala.util.Try

abstract class AsyncSource[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](config: AsyncSourceConfig[D, N, C])
  extends SqlSource[D, N, RowData, BindedStatementBuilder[List[Any]]]
  with Decoders
  with Encoders {

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

  private val pool = config.pool

  override def close = {
    Await.result(pool.close, Duration.Inf)
    ()
  }

  protected def connection: Connection

  private def withConnection[T](f: Connection => Future[T])(implicit ec: ExecutionContext) = f(connection)

  def probe(sql: String) =
    Try {
      Await.result(pool.sendQuery(sql), Duration.Inf)
    }

  def transaction[T](f: AsyncSource[D, N, C] => Future[T]) =
    pool.inTransaction(c => f(new TransactionalAsyncSource(config, c)))

  def execute(sql: String, bind: BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = identity, generated: Option[String] = None)(implicit ec: ExecutionContext) = {
    logger.info(sql)
    val (expanded, params) = bind(new SqlBindedStatementBuilder).build(sql)
    withConnection(_.sendPreparedStatement(config.expandAction(expanded, generated), params(List()))).map(config.extractActionResult(generated))
  }

  def executeBatch[T](sql: String, bindParams: T => BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = (_: T) => identity[BindedStatementBuilder[List[Any]]] _, generated: Option[String] = None)(implicit ec: ExecutionContext): ActionApply[T] = {
    def run(values: List[T]): Future[List[Long]] =
      values match {
        case Nil =>
          Future.successful(List())
        case value :: tail =>
          val (expanded, params) = bindParams(value)(new SqlBindedStatementBuilder).build(sql)
          logger.info(expanded.toString)
          withConnection(conn => conn.sendPreparedStatement(config.expandAction(expanded, generated), params(List())).map(config.extractActionResult(generated)))
            .flatMap(r => run(tail).map(r +: _))
      }
    new ActionApply(run _)
  }

  def query[T](sql: String, extractor: RowData => T = identity[RowData] _, bind: BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = identity)(implicit ec: ExecutionContext) = {
    val (expanded, params) = bind(new SqlBindedStatementBuilder).build(sql)
    logger.info(expanded.toString)
    withConnection(_.sendPreparedStatement(expanded, params(List()))).map {
      _.rows match {
        case Some(rows) => rows.map(extractor).toList
        case None       => List()
      }
    }
  }

  def querySingle[T](sql: String, extractor: RowData => T = identity[RowData] _, bind: BindedStatementBuilder[List[Any]] => BindedStatementBuilder[List[Any]] = identity)(implicit ec: ExecutionContext) = {
    query(sql, extractor, bind).map(handleSingleResult)
  }

}
