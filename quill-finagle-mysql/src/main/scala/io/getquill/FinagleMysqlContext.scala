package io.getquill

import java.util.TimeZone

import scala.util.Try

import org.slf4j.LoggerFactory

import com.twitter.finagle.exp.mysql.Client
import com.twitter.finagle.exp.mysql.OK
import com.twitter.finagle.exp.mysql.Parameter
import com.twitter.finagle.exp.mysql.Result
import com.twitter.finagle.exp.mysql.Row
import com.twitter.finagle.exp.mysql.Transactions
import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.util.Local
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger

import io.getquill.context.finagle.mysql.FinagleMysqlDecoders
import io.getquill.context.finagle.mysql.FinagleMysqlEncoders
import io.getquill.context.sql.SqlContext
import io.getquill.util.LoadConfig
import io.getquill.util.Messages.fail

class FinagleMysqlContext[N <: NamingStrategy](
  client:                             Client with Transactions,
  private[getquill] val dateTimezone: TimeZone                 = TimeZone.getDefault
)
  extends SqlContext[MySQLDialect, N]
  with FinagleMysqlDecoders
  with FinagleMysqlEncoders {

  def this(config: FinagleMysqlContextConfig) = this(config.client, config.dateTimezone)
  def this(config: Config) = this(FinagleMysqlContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[FinagleMysqlContext[_]]))

  override type PrepareRow = List[Parameter]
  override type ResultRow = Row

  override type RunQueryResult[T] = Future[List[T]]
  override type RunQuerySingleResult[T] = Future[T]
  override type RunActionResult = Future[Long]
  override type RunActionReturningResult[T] = Future[Long]
  override type RunBatchActionResult = Future[List[Long]]
  override type RunBatchActionReturningResult[T] = Future[List[Long]]

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

  def executeQuery[T](sql: String, prepare: List[Parameter] => List[Parameter] = identity, extractor: Row => T = identity[Row] _): Future[List[T]] = {
    logger.info(sql)
    withClient(_.prepare(sql).select(prepare(List()): _*)(extractor)).map(_.toList)
  }

  def executeQuerySingle[T](sql: String, prepare: List[Parameter] => List[Parameter] = identity, extractor: Row => T = identity[Row] _): Future[T] =
    executeQuery(sql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](sql: String, prepare: List[Parameter] => List[Parameter] = identity): Future[Long] = {
    logger.info(sql)
    withClient(_.prepare(sql)(prepare(List()): _*))
      .map(r => toOk(r).affectedRows)
  }

  def executeActionReturning[O](sql: String, prepare: List[Parameter] => List[Parameter] = identity, extractor: Row => O, returningColumn: String): Future[Long] = {
    logger.info(sql)
    withClient(_.prepare(sql)(prepare(List()): _*))
      .map(r => toOk(r).insertId)
  }

  def executeBatchAction[B](groups: List[BatchGroup]): Future[List[Long]] =
    Future.collect {
      groups.map {
        case BatchGroup(sql, prepare) =>
          prepare.foldLeft(Future.value(List.empty[Long])) {
            case (acc, prepare) =>
              acc.flatMap { list =>
                executeAction(sql, prepare).map(list :+ _)
              }
          }
      }
    }.map(_.flatten.toList)

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Row => T): Future[List[Long]] =
    Future.collect {
      groups.map {
        case BatchGroupReturning(sql, column, prepare) =>
          prepare.foldLeft(Future.value(List.empty[Long])) {
            case (acc, prepare) =>
              acc.flatMap { list =>
                executeActionReturning(sql, prepare, extractor, column).map(list :+ _)
              }
          }
      }
    }.map(_.flatten.toList)

  private def toOk(result: Result) =
    result match {
      case ok: OK => ok
      case error  => fail(error.toString)
    }

  private def withClient[T](f: Client => T) =
    currentClient().map {
      client => f(client)
    }.getOrElse {
      f(client)
    }
}
