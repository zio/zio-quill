package io.getquill.sources.finagle.mysql

import java.util.TimeZone
import com.twitter.finagle.exp.mysql.Client
import com.twitter.finagle.exp.mysql.Parameter
import com.twitter.finagle.exp.mysql.Result
import com.twitter.finagle.exp.mysql.Row
import com.twitter.util.Future
import com.twitter.util.Local
import com.typesafe.scalalogging.StrictLogging
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.SqlSource
import io.getquill.sources.sql.idiom.MySQLDialect
import scala.util.Success
import com.twitter.util.Await
import scala.util.Try
import com.twitter.finagle.Service
import com.twitter.finagle.exp.mysql.Request
import com.twitter.finagle.exp.mysql.PrepareRequest
import io.getquill.FinagleMysqlSourceConfig

class FinagleMysqlSource[N <: NamingStrategy](config: FinagleMysqlSourceConfig[N])
    extends SqlSource[MySQLDialect, N, Row, List[Parameter]]
    with FinagleMysqlDecoders
    with FinagleMysqlEncoders
    with StrictLogging {

  type QueryResult[T] = Future[List[T]]
  type ActionResult[T] = Future[Result]
  type BatchedActionResult[T] = Future[List[Result]]

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

  def execute(sql: String): Future[Result] =
    withClient(_.prepare(sql)())

  def execute[T](sql: String, bindParams: T => List[Parameter] => List[Parameter]): List[T] => Future[List[Result]] = {
    def run(values: List[T]): Future[List[Result]] =
      values match {
        case Nil =>
          Future.value(List())
        case value :: tail =>
          logger.info(sql)
          withClient(_.prepare(sql)(bindParams(value)(List()): _*))
            .flatMap(_ => run(tail))
      }
    run _
  }

  def query[T](sql: String, bind: List[Parameter] => List[Parameter], extractor: Row => T): Future[List[T]] = {
    logger.info(sql)
    withClient(_.prepare(sql).select(bind(List()): _*)(extractor)).map(_.toList)
  }

  private def withClient[T](f: Client => T) =
    currentClient().map {
      client => f(client)
    }.getOrElse {
      f(client)
    }
}
