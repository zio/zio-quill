package io.getquill.source.async

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
import io.getquill.source.sql.SqlSource
import io.getquill.source.sql.idiom.MySQLDialect
import io.getquill.source.sql.idiom.SqlIdiom
import language.experimental.macros
import io.getquill.quotation.Quoted
import io.getquill.source.sql.SqlSourceMacro

trait AsyncSource[D <: SqlIdiom, N <: NamingStrategy, C <: Connection]
    extends SqlSource[D, N, RowData, List[Any]]
    with Decoders
    with Encoders
    with StrictLogging {

  type QueryResult[T] = Future[List[T]]
  type ActionResult[T] = Future[DBQueryResult]
  type BatchedActionResult[T] = Future[List[DBQueryResult]]

  protected def objectFactory(config: Configuration): ObjectFactory[C]

  protected val pool = Pool(config, objectFactory)

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

  def execute(sql: String, bindList: List[List[Any] => List[Any]])(implicit ec: ExecutionContext): Future[List[DBQueryResult]] =
    bindList match {
      case Nil =>
        Future.successful(List())
      case bind :: tail =>
        logger.info(sql)
        withConnection(_.sendPreparedStatement(sql, bind(List())))
          .flatMap(_ => execute(sql, tail))
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
