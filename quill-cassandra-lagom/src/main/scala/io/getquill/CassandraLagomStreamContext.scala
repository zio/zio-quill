package io.getquill

import akka.stream.scaladsl.Source
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.getquill.context.ExecutionInfo
import io.getquill.util.ContextLogger

import scala.concurrent.ExecutionContext

class CassandraLagomStreamContext[N <: NamingStrategy](
  naming:  N,
  session: CassandraSession
) extends CassandraLagomSessionContext[N](naming, session) {

  override type Result[T] = Source[T, NotUsed]
  override type RunQuerySingleResult[T] = T
  override type RunQueryResult[T] = T
  override type RunActionResult = Done
  override type RunBatchActionResult = Done

  private val logger = ContextLogger(this.getClass)

  def executeQuery[T](
    cql:       String,
    prepare:   Prepare      = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  )(info: ExecutionInfo, dc: DatasourceContext)(implicit executionContext: ExecutionContext): Result[RunQueryResult[T]] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, wrappedSession, logger)
    val resultSource = statement.map(st => session.select(st).map(row => extractor(row, wrappedSession)))
    Source
      .fromFutureSource(resultSource)
      .mapMaterializedValue(_ => NotUsed)
  }

  def executeQuerySingle[T](
    cql:       String,
    prepare:   Prepare      = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  )(info: ExecutionInfo, dc: DatasourceContext)(
    implicit
    executionContext: ExecutionContext
  ): Result[RunQuerySingleResult[T]] = {
    executeQuery(cql, prepare, extractor)(info, dc).take(1)
  }

  def executeAction[T](cql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: DatasourceContext)(
    implicit
    executionContext: ExecutionContext
  ): Result[RunActionResult] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, CassandraLagomSession(session), logger)
    Source.fromFuture(statement).mapAsync(1) { st =>
      session.executeWrite(st)
    }
  }

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext)(
    implicit
    executionContext: ExecutionContext
  ): Result[RunBatchActionResult] = {
    val sourceList = groups.flatMap {
      case BatchGroup(cql, prepares) =>
        prepares.map(executeAction(cql, _)(info, dc))
    }
    Source(sourceList).flatMapConcat(identity)
  }

}
