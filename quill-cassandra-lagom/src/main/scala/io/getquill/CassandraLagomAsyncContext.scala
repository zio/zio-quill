package io.getquill

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.getquill.util.ContextLogger

import scala.concurrent.{ ExecutionContext, Future }

class CassandraLagomAsyncContext[N <: NamingStrategy](
  naming:  N,
  session: CassandraSession
)
  extends CassandraLagomSessionContext[N](naming, session) {

  override type Result[T] = Future[T]
  override type RunQuerySingleResult[T] = Option[T]
  override type RunQueryResult[T] = Seq[T]
  override type RunActionResult = Done
  override type RunBatchActionResult = Done

  private val logger = ContextLogger(this.getClass)

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit executionContext: ExecutionContext): Result[RunQueryResult[T]] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, logger)
    statement.flatMap(st => session.selectAll(st)).map(_.map(extractor))
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit executionContext: ExecutionContext): Result[RunQuerySingleResult[T]] = {
    executeQuery(cql, prepare, extractor).map(_.headOption)
  }

  def executeAction[T](cql: String, prepare: Prepare = identityPrepare)(implicit executionContext: ExecutionContext): Result[RunActionResult] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, logger)
    statement.flatMap(st => session.executeWrite(st))
  }

  def executeBatchAction(groups: List[BatchGroup])(implicit executionContext: ExecutionContext): Result[RunBatchActionResult] = {
    Future.sequence {
      groups.flatMap {
        case BatchGroup(cql, prepares) =>
          prepares.map(executeAction(cql, _))
      }
    }.map(_ => Done)
  }

}
