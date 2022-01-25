package io.getquill

import akka.stream.alpakka.cassandra.scaladsl.{ CassandraSession => CassandraAlpakkaSession }
import akka.stream.scaladsl.Source
import akka.{ Done, NotUsed }
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{ BoundStatement, PreparedStatement }
import io.getquill.context.cassandra.{ CassandraSessionContext, CqlIdiom, PrepareStatementCache }
import io.getquill.context.{ CassandraSession, ExecutionInfo, StreamingContext }
import io.getquill.monad.ScalaFutureIOMonad
import io.getquill.util.ContextLogger

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.Failure

class CassandraAlpakkaContext[N <: NamingStrategy](
  val naming:                     N,
  val alpakkaSession:             CassandraAlpakkaSession,
  val preparedStatementCacheSize: Long
) extends CassandraSessionContext[N]
  with CassandraSession
  with StreamingContext[CqlIdiom, N]
  with ScalaFutureIOMonad {

  private val logger = ContextLogger(classOf[CassandraAlpakkaContext[_]])

  override type StreamResult[T] = Source[T, NotUsed]
  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type Runner = Unit
  override type RunActionResult = Done
  override type RunBatchActionResult = Done

  override lazy val session: CqlSession = Await.result(alpakkaSession.underlying(), 30.seconds)

  lazy val asyncCache = new PrepareStatementCache[Future[PreparedStatement]](preparedStatementCacheSize)

  override def prepareAsync(cql: String)(implicit executionContext: ExecutionContext): Future[BoundStatement] = {
    val output = asyncCache(cql) { stmt =>
      alpakkaSession.prepare(stmt)
    }

    output.onComplete {
      case Failure(_) => asyncCache.invalidate(cql)
      case _          => ()
    }
    output.map(_.bind())
  }

  override def performIO[T](io: IO[T, _], transactional: Boolean = false)(implicit ec: ExecutionContext): Result[T] = {
    if (transactional) logger.underlying.warn("Cassandra doesn't support transactions, ignoring `io.transactional`")
    super.performIO(io)
  }

  def streamQuery[T](fetchSize: Option[Int], cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(
    info: ExecutionInfo,
    dc:   Runner
  )(implicit ec: ExecutionContext): StreamResult[T] = {
    alpakkaSession.select(prepareAsyncAndGetStatement(cql, prepare, this, logger)).map(row => extractor(row, this))
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(
    info: ExecutionInfo,
    dc:   Runner
  )(implicit executionContext: ExecutionContext): Result[RunQueryResult[T]] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, this, logger)
    statement.flatMap(st => alpakkaSession.selectAll(st)).map(rows => rows.map(row => extractor(row, this)).toList)
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(
    info: ExecutionInfo,
    dc:   Runner
  )(implicit executionContext: ExecutionContext): Result[RunQuerySingleResult[T]] = {
    executeQuery(cql, prepare, extractor)(info, dc).map(handleSingleResult)
  }

  def executeAction(cql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner)(
    implicit
    executionContext: ExecutionContext
  ): Result[RunActionResult] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, this, logger)
    statement.flatMap((st: BoundStatement) => alpakkaSession.executeWrite(st))
  }

  def executeBatchAction(
    groups: List[BatchGroup]
  )(info: ExecutionInfo, dc: Runner)(implicit executionContext: ExecutionContext): Result[RunBatchActionResult] = {
    Future
      .sequence {
        groups.flatMap {
          case BatchGroup(cql, prepare) =>
            prepare.map(executeAction(cql, _)(info, dc))
        }
      }
      .map(_ => Done)
  }

  override def close() = {
    alpakkaSession.close(scala.concurrent.ExecutionContext.Implicits.global)
    ()
  }

}
