package io.getquill

import akka.stream.scaladsl.Source
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.getquill.util.ContextLogger

import scala.annotation.nowarn
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

  @nowarn // Just for scala 2.13, there is deprecation warn on fromFutureSource missing in other scala versions
  def executeQuery[T](
    cql:       String,
    prepare:   Prepare      = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  )(implicit executionContext: ExecutionContext): Result[RunQueryResult[T]] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, logger)
    val resultSource = statement.map(st => session.select(st).map(extractor))
    Source
      .fromFutureSource(resultSource)
      .mapMaterializedValue(_ => NotUsed)
  }

  def executeQuerySingle[T](
    cql:       String,
    prepare:   Prepare      = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  )(
    implicit
    executionContext: ExecutionContext
  ): Result[RunQuerySingleResult[T]] = {
    executeQuery(cql, prepare, extractor).take(1)
  }

  @nowarn // Just for scala 2.13, there is deprecation warn on method missing in other scala versions
  def executeAction[T](cql: String, prepare: Prepare = identityPrepare)(
    implicit
    executionContext: ExecutionContext
  ): Result[RunActionResult] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, logger)
    Source.fromFuture(statement).mapAsync(1) { st =>
      session.executeWrite(st)
    }
  }

  def executeBatchAction(groups: List[BatchGroup])(
    implicit
    executionContext: ExecutionContext
  ): Result[RunBatchActionResult] = {
    val sourceList = groups.flatMap {
      case BatchGroup(cql, prepares) =>
        prepares.map(executeAction(cql, _))
    }
    Source(sourceList).flatMapConcat(identity)
  }

}
