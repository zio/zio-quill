package io.getquill

import com.datastax.oss.driver.api.core.{ CqlSession, CqlSessionBuilder }
import com.typesafe.config.Config
import io.getquill.context.ExecutionInfo
import io.getquill.context.cassandra.util.FutureConversions._
import io.getquill.monad.ScalaFutureIOMonad
import io.getquill.util.{ ContextLogger, LoadConfig }

import scala.jdk.CollectionConverters._
import scala.compat.java8.FutureConverters._

import scala.concurrent.{ ExecutionContext, Future }

class CassandraAsyncContext[N <: NamingStrategy](
  naming:                     N,
  session:                    CqlSession,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends CassandraCqlSessionContext[N](naming, session, keyspace, preparedStatementCacheSize)
  with ScalaFutureIOMonad {

  def this(naming: N, config: CassandraContextConfig) = this(naming, config.session, config.keyspace, config.preparedStatementCacheSize)

  def this(naming: N, config: Config) = this(naming, CassandraContextConfig(config))

  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  private val logger = ContextLogger(classOf[CassandraAsyncContext[_]])

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Unit
  override type RunBatchActionResult = Unit
  override type DatasourceContext = Unit

  override def performIO[T](io: IO[T, _], transactional: Boolean = false)(implicit ec: ExecutionContext): Result[T] = {
    if (transactional) logger.underlying.warn("Cassandra doesn't support transactions, ignoring `io.transactional`")
    super.performIO(io)
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext)(implicit executionContext: ExecutionContext): Result[RunQueryResult[T]] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, this, logger)

    //statement.flatMap(st => session.executeAsync(st).asScala)
    //.map(result=>_.all.asScala.toList.map(row => extractor(row, this)))
    //TODO: 3.x implementation collected all data to memory - technically no difference between sync and async execution
    //      4x returns data (rows) asynchronously row by row. Details:
    //      https://docs.datastax.com/en/developer/java-driver/2.1/manual/async/#async-paging
    statement.map(st => session.execute(st).asScala.toList.map(row => extractor(row, this)))
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext)(implicit executionContext: ExecutionContext): Result[RunQuerySingleResult[T]] = {
    executeQuery(cql, prepare, extractor)(info, dc).map(handleSingleResult)
  }

  def executeAction[T](cql: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: DatasourceContext)(implicit executionContext: ExecutionContext): Result[RunActionResult] = {
    val statement = prepareAsyncAndGetStatement(cql, prepare, this, logger)
    statement.flatMap(st => session.executeAsync(st).toCompletableFuture.toScala).map(_ => ())
  }

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: DatasourceContext)(implicit executionContext: ExecutionContext): Result[RunBatchActionResult] = {
    Future.sequence {
      groups.flatMap {
        case BatchGroup(cql, prepare) =>
          prepare.map(executeAction(cql, _)(info, dc))
      }
    }.map(_ => ())
  }
}
