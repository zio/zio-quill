package io.getquill.sources.async

import scala.annotation.implicitNotFound
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Try
import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.github.mauricio.async.db.RowData
import com.github.mauricio.async.db.pool.ObjectFactory
import com.typesafe.scalalogging.StrictLogging
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.SqlSource
import io.getquill.sources.sql.idiom.MySQLDialect
import io.getquill.sources.sql.idiom.SqlIdiom
import language.experimental.macros
import io.getquill.quotation.Quoted
import io.getquill.sources.sql.SqlSourceMacro

class AsyncSource[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](config: AsyncSourceConfig[D, N, C])
    extends SqlSource[D, N, RowData, List[Any]]
    with Decoders
    with Encoders
    with StrictLogging {

  type QueryResult[T] = Future[List[T]]
  type ActionResult[T] = Future[DBQueryResult]
  type BatchedActionResult[T] = Future[List[DBQueryResult]]

  private val pool = config.pool

  override def close = {
    Await.result(pool.close, Duration.Inf)
    ()
  }

  private def withConnection[T](f: Connection => Future[T])(implicit ec: ExecutionContext) =
    ec match {
      case TransactionalExecutionContext(ec, conn) => f(conn)
      case other                                   => f(pool)
    }

  def probe(sql: String) =
    Try {
      Await.result(pool.sendQuery(sql), Duration.Inf)
    }

  def transaction[T](f: TransactionalExecutionContext => Future[T])(implicit ec: ExecutionContext) =
    pool.inTransaction { c =>
      f(TransactionalExecutionContext(ec, c))
    }

  def execute(sql: String)(implicit ec: ExecutionContext) = {
    logger.info(sql)
    withConnection(_.sendQuery(sql))
  }

  def execute[T](sql: String, bindParams: T => List[Any] => List[Any])(implicit ec: ExecutionContext): List[T] => Future[List[DBQueryResult]] = {
    def run(values: List[T]): Future[List[DBQueryResult]] =
      values match {
        case Nil =>
          Future.successful(List())
        case value :: tail =>
          logger.info(sql)
          withConnection(_.sendPreparedStatement(sql, bindParams(value)(List())))
            .flatMap(_ => run(tail))
      }
    run _
  }

  def query[T](sql: String, bind: List[Any] => List[Any], extractor: RowData => T)(implicit ec: ExecutionContext) = {
    withConnection(_.sendPreparedStatement(sql, bind(List()))).map {
      _.rows match {
        case Some(rows) => rows.map(extractor).toList
        case None       => List()
      }
    }
  }
}
