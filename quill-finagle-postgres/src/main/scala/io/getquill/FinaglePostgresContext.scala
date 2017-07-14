package io.getquill

import com.twitter.util.{ Await, Future, Local }
import com.twitter.finagle.postgres._
import com.typesafe.config.Config
import io.getquill.context.finagle.postgres._
import io.getquill.context.sql.SqlContext
import io.getquill.util.{ ContextLogger, LoadConfig }
import scala.util.Try

class FinaglePostgresContext[N <: NamingStrategy](client: PostgresClient) extends SqlContext[FinaglePostgresDialect, N] with FinaglePostgresEncoders with FinaglePostgresDecoders {

  def this(config: FinaglePostgresContextConfig) = this(config.client)
  def this(config: Config) = this(FinaglePostgresContextConfig(config))
  def this(configPrefix: String) = {
    this(LoadConfig(configPrefix))
  }

  private val logger = ContextLogger(classOf[FinaglePostgresContext[_]])

  override type PrepareRow = List[Param[_]]
  override type ResultRow = Row
  override type RunQueryResult[T] = Future[List[T]]
  override type RunQuerySingleResult[T] = Future[T]
  override type RunActionResult = Future[Long]
  override type RunActionReturningResult[T] = Future[T]
  override type RunBatchActionResult = Future[List[Long]]
  override type RunBatchActionReturningResult[T] = Future[List[T]]

  private val currentClient = new Local[PostgresClient]

  override def close = Await.result(client.close())

  private def expandAction(sql: String, returningColumn: String): String =
    s"$sql RETURNING $returningColumn"

  def probe(sql: String) = Try(Await.result(client.query(sql)))

  def transaction[T](f: => Future[T]) = client.inTransaction { c =>
    currentClient.update(c)
    f.ensure(currentClient.clear)
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
        prepare.foldLeft(Future.value(List.empty[Long])) {
          case (acc, prepare) =>
            acc.flatMap { list =>
              executeAction(sql, prepare).map(list :+ _)
            }
        }
    }
  }.map(_.flatten.toList)

  def executeActionReturning[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T], returningColumn: String): Future[T] = {
    val (params, prepared) = prepare(Nil)
    logger.logQuery(sql, params)
    withClient(_.prepareAndQuery(expandAction(sql, returningColumn), prepared: _*)(extractor)).map(v => handleSingleResult(v.toList))
  }

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): Future[List[T]] =
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

  private def withClient[T](f: PostgresClient => T) =
    currentClient().map(f).getOrElse(f(client))
}
