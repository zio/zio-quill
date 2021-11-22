package io.getquill

import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.getquill.context.ExecutionInfo
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
  override type Session = CassandraLagomSession

  private val logger = ContextLogger(this.getClass)

  def prepareAction[T](cql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: DatasourceContext)(implicit executionContext: ExecutionContext): CassandraLagomSession => Future[BoundStatement] = (session: Session) => {
    val prepareResult = session.cs.prepare(cql).map(bs => prepare(bs.bind(), session))
    val preparedRow = prepareResult.map {
      case (params, bs) =>
        logger.logQuery(cql, params)
        bs
    }
    preparedRow
  }

  def prepareBatchAction[T](groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext)(implicit executionContext: ExecutionContext): CassandraLagomSession => Future[List[BoundStatement]] = (session: Session) => {
    val batches = groups.flatMap {
      case BatchGroup(cql, prepares) =>
        prepares.map(cql -> _)
    }
    Future.traverse(batches) {
      case (cql, prepare) =>
        val prepareCql = prepareAction(cql, prepare)(info, dc)
        prepareCql(session)
    }
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext)(implicit executionContext: ExecutionContext): Result[RunQueryResult[T]] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, wrappedSession, logger)
    statement.flatMap(st => session.selectAll(st)).map(_.map(row => extractor(row, wrappedSession)))
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext)(implicit executionContext: ExecutionContext): Result[RunQuerySingleResult[T]] = {
    executeQuery(cql, prepare, extractor)(info, dc).map(_.headOption)
  }

  def executeAction(cql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: DatasourceContext)(implicit executionContext: ExecutionContext): Result[RunActionResult] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, wrappedSession, logger)
    statement.flatMap(st => session.executeWrite(st))
  }

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext)(implicit executionContext: ExecutionContext): Result[RunBatchActionResult] = {
    Future.sequence {
      groups.flatMap {
        case BatchGroup(cql, prepares) =>
          prepares.map(executeAction(cql, _)(info, dc))
      }
    }.map(_ => Done)
  }

}
