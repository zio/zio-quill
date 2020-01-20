package io.getquill

import java.util.TimeZone

import scala.util.Try
import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.mysql.Client
import com.twitter.finagle.mysql.LongValue
import com.twitter.finagle.mysql.OK
import com.twitter.finagle.mysql.Parameter
import com.twitter.finagle.mysql.{ Result => MysqlResult }
import com.twitter.finagle.mysql.Row
import com.twitter.finagle.mysql.Transactions
import com.twitter.finagle.mysql.TimestampValue
import com.twitter.finagle.mysql.IsolationLevel
import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.util.Local
import com.typesafe.config.Config
import io.getquill.context.finagle.mysql.FinagleMysqlDecoders
import io.getquill.context.finagle.mysql.FinagleMysqlEncoders
import io.getquill.context.finagle.mysql.SingleValueRow
import io.getquill.context.sql.SqlContext
import io.getquill.util.{ ContextLogger, LoadConfig }
import io.getquill.util.Messages.fail
import io.getquill.monad.TwitterFutureIOMonad
import io.getquill.context.{ Context, StreamingContext, TranslateContext }

sealed trait OperationType
object OperationType {
  case object Read extends OperationType
  case object Write extends OperationType
}

class FinagleMysqlContext[N <: NamingStrategy](
  val naming:                               N,
  client:                                   OperationType => Client with Transactions,
  private[getquill] val injectionTimeZone:  TimeZone,
  private[getquill] val extractionTimeZone: TimeZone
)
  extends Context[MySQLDialect, N]
  with TranslateContext
  with SqlContext[MySQLDialect, N]
  with StreamingContext[MySQLDialect, N]
  with FinagleMysqlDecoders
  with FinagleMysqlEncoders
  with TwitterFutureIOMonad {

  import OperationType._

  def this(naming: N, client: Client with Transactions, injectionTimeZone: TimeZone, extractionTimeZone: TimeZone) =
    this(naming, _ => client, injectionTimeZone, extractionTimeZone)

  def this(naming: N, master: Client with Transactions, slave: Client with Transactions, timeZone: TimeZone) = {
    this(naming, _ match {
      case OperationType.Read  => slave
      case OperationType.Write => master
    }, timeZone, timeZone)
  }

  def this(naming: N, config: FinagleMysqlContextConfig) = this(naming, config.client, config.injectionTimeZone, config.extractionTimeZone)
  def this(naming: N, config: Config) = this(naming, FinagleMysqlContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  def this(naming: N, client: Client with Transactions, timeZone: TimeZone) = this(naming, client, timeZone, timeZone)
  def this(naming: N, config: FinagleMysqlContextConfig, timeZone: TimeZone) = this(naming, config.client, timeZone)
  def this(naming: N, config: Config, timeZone: TimeZone) = this(naming, FinagleMysqlContextConfig(config), timeZone)
  def this(naming: N, configPrefix: String, timeZone: TimeZone) = this(naming, LoadConfig(configPrefix), timeZone)

  def this(naming: N, config: FinagleMysqlContextConfig, injectionTimeZone: TimeZone, extractionTimeZone: TimeZone) = this(naming, config.client, injectionTimeZone, extractionTimeZone)
  def this(naming: N, config: Config, injectionTimeZone: TimeZone, extractionTimeZone: TimeZone) = this(naming, FinagleMysqlContextConfig(config), injectionTimeZone, extractionTimeZone)
  def this(naming: N, configPrefix: String, injectionTimeZone: TimeZone, extractionTimeZone: TimeZone) = this(naming, LoadConfig(configPrefix), injectionTimeZone, extractionTimeZone)

  val idiom = MySQLDialect

  private val logger = ContextLogger(classOf[FinagleMysqlContext[_]])

  override type PrepareRow = List[Parameter]
  override type ResultRow = Row

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]
  override type StreamResult[T] = Future[AsyncStream[T]]

  protected val timestampValue =
    new TimestampValue(
      injectionTimeZone,
      extractionTimeZone
    )

  override def close =
    Await.result(
      Future.join(
        client(Write).close(),
        client(Read).close()
      ).unit
    )

  private val currentClient = new Local[Client]

  def probe(sql: String) =
    Try(Await.result(client(Write).query(sql)))

  def transaction[T](f: => Future[T]) =
    client(Write).transaction {
      transactional =>
        currentClient.update(transactional)
        f.ensure(currentClient.clear)
    }

  def transactionWithIsolation[T](isolationLevel: IsolationLevel)(f: => Future[T]) =
    client(Write).transactionWithIsolation(isolationLevel) {
      transactional =>
        currentClient.update(transactional)
        f.ensure(currentClient.clear)
    }

  override def performIO[T](io: IO[T, _], transactional: Boolean = false): Result[T] =
    transactional match {
      case false => super.performIO(io)
      case true  => transaction(super.performIO(io))
    }

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Future[List[T]] = {
    val (params, prepared) = prepare(Nil)
    logger.logQuery(sql, params)
    withClient(Read)(_.prepare(sql).select(prepared: _*)(extractor)).map(_.toList)
  }

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Future[T] =
    executeQuery(sql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare): Future[Long] = {
    val (params, prepared) = prepare(Nil)
    logger.logQuery(sql, params)
    withClient(Write)(_.prepare(sql)(prepared: _*))
      .map(r => toOk(r).affectedRows)
  }

  def executeActionReturning[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T], returningAction: ReturnAction): Future[T] = {
    val (params, prepared) = prepare(Nil)
    logger.logQuery(sql, params)
    withClient(Write)(_.prepare(sql)(prepared: _*))
      .map(extractReturningValue(_, extractor))
  }

  def executeBatchAction[B](groups: List[BatchGroup]): Future[List[Long]] =
    Future.collect {
      groups.map {
        case BatchGroup(sql, prepare) =>
          prepare.foldLeft(Future.value(List.newBuilder[Long])) {
            case (acc, prepare) =>
              acc.flatMap { list =>
                executeAction(sql, prepare).map(list += _)
              }
          }.map(_.result())
      }
    }.map(_.flatten.toList)

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): Future[List[T]] =
    Future.collect {
      groups.map {
        case BatchGroupReturning(sql, column, prepare) =>
          prepare.foldLeft(Future.value(List.newBuilder[T])) {
            case (acc, prepare) =>
              acc.flatMap { list =>
                executeActionReturning(sql, prepare, extractor, column).map(list += _)
              }
          }.map(_.result())
      }
    }.map(_.flatten.toList)

  def streamQuery[T](fetchSize: Option[Int], sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Future[AsyncStream[T]] = {
    val rowsPerFetch = fetchSize.getOrElse(20)
    val (params: List[Any], prepared: List[Parameter]) = prepare(Nil)
    logger.logQuery(sql, params)

    withClient(Read) { client =>
      client.cursor(sql)(rowsPerFetch, prepared: _*)(extractor).map(_.stream)
    }
  }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] = {
    prepare(Nil)._2.map(param => prepareParam(param.value))
  }

  private def extractReturningValue[T](result: MysqlResult, extractor: Extractor[T]) =
    extractor(SingleValueRow(LongValue(toOk(result).insertId)))

  protected def toOk(result: MysqlResult) =
    result match {
      case ok: OK => ok
      case error  => fail(error.toString)
    }

  def withClient[T](op: OperationType)(f: Client => T) =
    currentClient().map {
      client => f(client)
    }.getOrElse {
      f(client(op))
    }
}
