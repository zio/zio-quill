package io.getquill

import com.twitter.util.{ Await, Future, Local }
import com.twitter.finagle.postgres._
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import io.getquill.context.finagle.postgres._
import io.getquill.context.sql.SqlContext
import io.getquill.util.LoadConfig
import org.slf4j.LoggerFactory
import scala.util.Try
import io.getquill.monad.TwitterFutureIOMonad

class FinaglePostgresContext[N <: NamingStrategy](client: Client)
  extends SqlContext[FinaglePostgresDialect, N]
  with FinaglePostgresEncoders
  with FinaglePostgresDecoders
  with TwitterFutureIOMonad {

  def this(config: FinaglePostgresContextConfig) = this(config.client)
  def this(config: Config) = this(FinaglePostgresContextConfig(config))
  def this(configPrefix: String) = {
    this(LoadConfig(configPrefix))
  }

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(classOf[FinaglePostgresContext[_]]))

  override type PrepareRow = List[Param[_]]
  override type ResultRow = Row

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  private val currentClient = new Local[Client]

  override def close = Await.result(client.close())

  private def expandAction(sql: String, returningColumn: String): String =
    s"$sql RETURNING $returningColumn"

  def probe(sql: String) = Try(Await.result(client.query(sql)))

  def transaction[T](f: => Future[T]) =
    client.inTransaction { c =>
      currentClient.update(c)
      f.ensure(currentClient.clear)
    }

  override def unsafePerformIO[T](io: IO[T, _], transactional: Boolean = false): Result[T] =
    transactional match {
      case false => super.unsafePerformIO(io)
      case true  => transaction(super.unsafePerformIO(io))
    }

  def executeQuery[T](sql: String, prepare: PrepareRow => PrepareRow = identity, extractor: Row => T = identity[Row] _): Future[List[T]] = {
    logger.info(sql)
    withClient(_.prepareAndQuery(sql, prepare(Nil): _*)(extractor).map(_.toList))
  }

  def executeQuerySingle[T](sql: String, prepare: PrepareRow => PrepareRow = identity, extractor: Row => T = identity[Row] _): Future[T] =
    executeQuery(sql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](sql: String, prepare: PrepareRow => PrepareRow = identity, extractor: Row => T = identity[Row] _): Future[Long] = {
    logger.info(sql)
    withClient(_.prepareAndExecute(sql, prepare(Nil): _*)).map(_.toLong)
  }

  def executeBatchAction[B](groups: List[BatchGroup]): Future[List[Long]] = Future.collect {
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

  def executeActionReturning[T](sql: String, prepare: PrepareRow => PrepareRow = identity, extractor: Row => T, returningColumn: String): Future[T] = {
    logger.info(sql)
    withClient(_.prepareAndQuery(expandAction(sql, returningColumn), prepare(List()): _*)(extractor)).map(v => handleSingleResult(v.toList))
  }

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Row => T): Future[List[T]] =
    Future.collect {
      groups.map {
        case BatchGroupReturning(sql, column, prepare) =>
          prepare.foldLeft(Future.value(List.empty[T])) {
            case (acc, prepare) =>
              acc.flatMap { list =>
                executeActionReturning(sql, prepare, extractor, column).map(list :+ _)
              }
          }
      }
    }.map(_.flatten.toList)

  private def withClient[T](f: Client => T) =
    currentClient().map(f).getOrElse(f(client))
}
