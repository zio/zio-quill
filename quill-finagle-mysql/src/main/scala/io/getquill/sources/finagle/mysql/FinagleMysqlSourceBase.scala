package io.getquill.sources.finagle.mysql

import com.twitter.finagle.exp.mysql.{ Client, Error, OK, Parameter, Result, Row }
import com.twitter.util.{ Await, Future }
import com.typesafe.scalalogging.Logger
import io.getquill.naming.NamingStrategy
import io.getquill.sources.BindedStatementBuilder
import io.getquill.sources.sql.idiom.MySQLDialect
import io.getquill.sources.sql.{ SqlBindedStatementBuilder, SqlSource }
import org.slf4j.LoggerFactory

import scala.util.Try
import java.util.TimeZone

abstract class FinagleMysqlSourceBase[N <: NamingStrategy]
  extends SqlSource[MySQLDialect, N, Row, BindedStatementBuilder[List[Parameter]]]
  with FinagleMysqlDecoders
  with FinagleMysqlEncoders {

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[FinagleMysqlSourceBase[_]]))

  type QueryResult[T] = Future[List[T]]
  type SingleQueryResult[T] = Future[T]
  type ActionResult[T] = Future[Long]
  type BatchedActionResult[T] = Future[List[Long]]

  class ActionApply[T](f: List[T] => Future[List[Long]])
    extends Function1[List[T], Future[List[Long]]] {
    def apply(params: List[T]) = f(params)

    def apply(param: T) = f(List(param)).map(_.head)
  }

  protected[mysql] def dateTimezone: TimeZone

  protected def client: Client

  override def close = Await.result(client.close())

  def probe(sql: String) =
    Try(Await.result(client.query(sql)))

  def executeAction(sql: String, bind: BindedStatementBuilder[List[Parameter]] => BindedStatementBuilder[List[Parameter]] = identity, generated: Option[String] = None): Future[Long] = {
    val (expanded, params) = bind(new SqlBindedStatementBuilder).build(sql)
    logger.info(expanded)
    client.prepare(expanded)(params(List()): _*).map(resultToLong(_, generated))
  }

  def executeActionBatch[T](sql: String, bindParams: T => BindedStatementBuilder[List[Parameter]] => BindedStatementBuilder[List[Parameter]] = (_: T) => identity[BindedStatementBuilder[List[Parameter]]] _, generated: Option[String] = None): ActionApply[T] = {
    def run(values: List[T]): Future[List[Long]] =
      values match {
        case Nil =>
          Future.value(List())
        case value :: tail =>
          val (expanded, params) = bindParams(value)(new SqlBindedStatementBuilder).build(sql)
          logger.info(expanded)
          client.prepare(expanded)(params(List()): _*)
            .map(resultToLong(_, generated))
            .flatMap(r => run(tail).map(r +: _))
      }
    new ActionApply(run _)
  }

  def executeQuery[T](sql: String, extractor: Row => T = identity[Row] _, bind: BindedStatementBuilder[List[Parameter]] => BindedStatementBuilder[List[Parameter]] = identity): Future[List[T]] = {
    val (expanded, params) = bind(new SqlBindedStatementBuilder).build(sql)
    logger.info(expanded)
    client.prepare(expanded).select(params(List()): _*)(extractor).map(_.toList)
  }

  def executeQuerySingle[T](sql: String, extractor: Row => T = identity[Row] _, bind: BindedStatementBuilder[List[Parameter]] => BindedStatementBuilder[List[Parameter]]): Future[T] =
    executeQuery(sql, extractor, bind).map(handleSingleResult)

  private def resultToLong(result: Result, generated: Option[String]) =
    result match {
      case ok: OK if (generated.isDefined) => ok.insertId
      case ok: OK                          => ok.affectedRows
      case error: Error                    => throw new IllegalStateException(error.toString)
    }
}
