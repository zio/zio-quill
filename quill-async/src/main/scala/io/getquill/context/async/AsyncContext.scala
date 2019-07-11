package io.getquill.context.async

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.github.mauricio.async.db.RowData
import com.github.mauricio.async.db.pool.PartitionedConnectionPool

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Try
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.{ NamingStrategy, ReturnAction }
import io.getquill.util.ContextLogger
import io.getquill.monad.ScalaFutureIOMonad
import io.getquill.context.{ Context, TranslateContext }

abstract class AsyncContext[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](val idiom: D, val naming: N, pool: PartitionedConnectionPool[C])
  extends Context[D, N]
  with TranslateContext
  with SqlContext[D, N]
  with Decoders
  with Encoders
  with ScalaFutureIOMonad {

  private val logger = ContextLogger(classOf[AsyncContext[_, _, _]])

  override type PrepareRow = List[Any]
  override type ResultRow = RowData

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  override def close = {
    Await.result(pool.close, Duration.Inf)
    ()
  }

  protected def withConnection[T](f: Connection => Future[T])(implicit ec: ExecutionContext) =
    ec match {
      case TransactionalExecutionContext(ec, conn) => f(conn)
      case other                                   => f(pool)
    }

  protected def extractActionResult[O](returningAction: ReturnAction, extractor: Extractor[O])(result: DBQueryResult): O

  protected def expandAction(sql: String, returningAction: ReturnAction) = sql

  def probe(sql: String) =
    Try {
      Await.result(pool.sendQuery(sql), Duration.Inf)
    }

  def transaction[T](f: TransactionalExecutionContext => Future[T])(implicit ec: ExecutionContext) =
    pool.inTransaction { c =>
      f(TransactionalExecutionContext(ec, c))
    }

  override def performIO[T](io: IO[T, _], transactional: Boolean = false)(implicit ec: ExecutionContext): Result[T] =
    transactional match {
      case false => super.performIO(io)
      case true  => transaction(super.performIO(io)(_))
    }

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext): Future[List[T]] = {
    val (params, values) = prepare(Nil)
    logger.logQuery(sql, params)
    withConnection(_.sendPreparedStatement(sql, values)).map {
      _.rows match {
        case Some(rows) => rows.map(extractor).toList
        case None       => Nil
      }
    }
  }

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext): Future[T] =
    executeQuery(sql, prepare, extractor).map(handleSingleResult)

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare)(implicit ec: ExecutionContext): Future[Long] = {
    val (params, values) = prepare(Nil)
    logger.logQuery(sql, params)
    withConnection(_.sendPreparedStatement(sql, values)).map(_.rowsAffected)
  }

  def executeActionReturning[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T], returningAction: ReturnAction)(implicit ec: ExecutionContext): Future[T] = {
    val expanded = expandAction(sql, returningAction)
    val (params, values) = prepare(Nil)
    logger.logQuery(sql, params)
    withConnection(_.sendPreparedStatement(expanded, values))
      .map(extractActionResult(returningAction, extractor))
  }

  def executeBatchAction(groups: List[BatchGroup])(implicit ec: ExecutionContext): Future[List[Long]] =
    Future.sequence {
      groups.map {
        case BatchGroup(sql, prepare) =>
          prepare.foldLeft(Future.successful(List.newBuilder[Long])) {
            case (acc, prepare) =>
              acc.flatMap { list =>
                executeAction(sql, prepare).map(list += _)
              }
          }.map(_.result())
      }
    }.map(_.flatten.toList)

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(implicit ec: ExecutionContext): Future[List[T]] =
    Future.sequence {
      groups.map {
        case BatchGroupReturning(sql, column, prepare) =>
          prepare.foldLeft(Future.successful(List.newBuilder[T])) {
            case (acc, prepare) =>
              acc.flatMap { list =>
                executeActionReturning(sql, prepare, extractor, column).map(list += _)
              }
          }.map(_.result())
      }
    }.map(_.flatten.toList)

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] = {
    prepare(Nil)._2.map(param => prepareParam(param))
  }
}
