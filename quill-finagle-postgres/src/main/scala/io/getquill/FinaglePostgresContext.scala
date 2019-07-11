package io.getquill

import com.twitter.util.{ Await, Future, Local }
import com.twitter.finagle.postgres._
import com.typesafe.config.Config
import io.getquill.ReturnAction.{ ReturnColumns, ReturnNothing, ReturnRecord }
import io.getquill.context.finagle.postgres._
import io.getquill.context.sql.SqlContext
import io.getquill.util.{ ContextLogger, LoadConfig }

import scala.util.Try
import io.getquill.context.{ Context, TranslateContext }
import io.getquill.monad.TwitterFutureIOMonad

class FinaglePostgresContext[N <: NamingStrategy](val naming: N, client: PostgresClient)
  extends Context[FinaglePostgresDialect, N]
  with TranslateContext
  with SqlContext[FinaglePostgresDialect, N]
  with FinaglePostgresEncoders
  with FinaglePostgresDecoders
  with TwitterFutureIOMonad {

  import FinaglePostgresContext._

  def this(naming: N, config: FinaglePostgresContextConfig) = this(naming, config.client)
  def this(naming: N, config: Config) = this(naming, FinaglePostgresContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  val idiom = FinaglePostgresDialect

  private val logger = ContextLogger(classOf[FinaglePostgresContext[_]])

  override type PrepareRow = List[Param[_]]
  override type ResultRow = Row

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  private val currentClient = new Local[PostgresClient]

  override def close = Await.result(client.close())

  private def expandAction(sql: String, returningAction: ReturnAction): String =
    returningAction match {
      // The Postgres dialect will create SQL that has a 'RETURNING' clause so we don't have to add one.
      case ReturnRecord           => s"$sql"
      // The Postgres dialect will not actually use these below variants but in case we decide to plug
      // in some other dialect into this context...
      case ReturnColumns(columns) => s"$sql RETURNING ${columns.mkString(", ")}"
      case ReturnNothing          => s"$sql"
    }

  def probe(sql: String) = Try(Await.result(client.query(sql)))

  // Only create a client if none exists to allow nested transactions.
  def transaction[T](f: => Future[T]) = currentClient() match {
    case None =>
      client.inTransaction { c =>
        currentClient.let(c) { f }
      }
    case Some(c) =>
      f
  }

  override def performIO[T](io: IO[T, _], transactional: Boolean = false): Result[T] =
    transactional match {
      case false => super.performIO(io)
      case true  => transaction(super.performIO(io))
    }

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Future[List[T]] = {
    val (params, prepared) = prepare(Nil)
    logger.logQuery(sql, params)
    withClient(_.prepareAndQuery(sql, prepared: _*)(extractor).map(_.toList))
  }

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Future[T] =
    executeQuery(sql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Future[Long] = {
    val (params, prepared) = prepare(Nil)
    logger.logQuery(sql, params)
    withClient(_.prepareAndExecute(sql, prepared: _*)).map(_.toLong)
  }

  def executeBatchAction[B](groups: List[BatchGroup]): Future[List[Long]] = Future.collect {
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

  def executeActionReturning[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T], returningAction: ReturnAction): Future[T] = {
    val (params, prepared) = prepare(Nil)
    logger.logQuery(sql, params)
    withClient(_.prepareAndQuery(expandAction(sql, returningAction), prepared: _*)(extractor)).map(v => handleSingleResult(v.toList))
  }

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

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] = {
    prepare(Nil)._2.map(param => prepareParam(param.encode()))
  }

  private def withClient[T](f: PostgresClient => T) =
    currentClient().map(f).getOrElse(f(client))
}

object FinaglePostgresContext {
  implicit class EncodeParam[T](val param: Param[T]) extends AnyVal {
    def encode(): Option[String] = param.encoder.encodeText(param.value)
  }
}
