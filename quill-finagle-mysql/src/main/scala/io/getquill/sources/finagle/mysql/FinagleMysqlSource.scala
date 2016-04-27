package io.getquill.sources.finagle.mysql

import com.twitter.finagle.exp.mysql.Client
import com.twitter.finagle.exp.mysql.Parameter
import com.twitter.finagle.exp.mysql.Result
import com.twitter.finagle.exp.mysql.Row
import com.twitter.util.Future
import com.twitter.util.Local
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.{ SqlBindedStatementBuilder, SqlSource }
import io.getquill.sources.sql.idiom.MySQLDialect

import com.twitter.util.Await
import scala.util.Try

import io.getquill.FinagleMysqlSourceConfig
import io.getquill.sources.BindedStatementBuilder
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

class FinagleMysqlSource[N <: NamingStrategy](config: FinagleMysqlSourceConfig[N])
  extends SqlSource[MySQLDialect, N, Row, BindedStatementBuilder[List[Parameter]]]
  with FinagleMysqlDecoders
  with FinagleMysqlEncoders {

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[FinagleMysqlSource[_]]))

  type QueryResult[T] = Future[List[T]]
  type ActionResult[T] = Future[Result]
  type BatchedActionResult[T] = Future[List[Result]]

  class ActionApply[T](f: List[T] => Future[List[Result]])
    extends Function1[List[T], Future[List[Result]]] {
    def apply(params: List[T]) = f(params)
    def apply(param: T) = f(List(param)).map(_.head)
  }

  private[mysql] def dateTimezone = config.dateTimezone

  private val client = config.client

  Await.result(client.ping)

  override def close = Await.result(client.close())

  private val currentClient = new Local[Client]

  def probe(sql: String) =
    Try(Await.result(client.query(sql)))

  def transaction[T](f: => Future[T]) =
    client.transaction {
      transactional =>
        currentClient.update(transactional)
        f.ensure(currentClient.clear)
    }

  def execute(sql: String, bind: BindedStatementBuilder[List[Parameter]] => BindedStatementBuilder[List[Parameter]], generated: Option[String] = None): Future[Result] = {
    val (expanded, params) = bind(new SqlBindedStatementBuilder).build(sql)
    logger.info(expanded)
    withClient(_.prepare(expanded)(params(List()): _*))
  }

  def executeBatch[T](sql: String, bindParams: T => BindedStatementBuilder[List[Parameter]] => BindedStatementBuilder[List[Parameter]], generated: Option[String] = None): ActionApply[T] = {
    def run(values: List[T]): Future[List[Result]] =
      values match {
        case Nil =>
          Future.value(List())
        case value :: tail =>
          val (expanded, params) = bindParams(value)(new SqlBindedStatementBuilder).build(sql)
          logger.info(expanded)
          withClient(_.prepare(expanded)(params(List()): _*))
            .flatMap(r => run(tail).map(r +: _))
      }
    new ActionApply(run _)
  }

  def query[T](sql: String, bind: BindedStatementBuilder[List[Parameter]] => BindedStatementBuilder[List[Parameter]], extractor: Row => T): Future[List[T]] = {
    val (expanded, params) = bind(new SqlBindedStatementBuilder).build(sql)
    logger.info(expanded)
    withClient(_.prepare(expanded).select(params(List()): _*)(extractor)).map(_.toList)
  }

  private def withClient[T](f: Client => T) =
    currentClient().map {
      client => f(client)
    }.getOrElse {
      f(client)
    }
}
