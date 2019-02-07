package io.getquill

import com.datastax.driver.core.Cluster
import com.typesafe.config.Config
import io.getquill.context.cassandra.{CassandraFutureContextEffect, CassandraSessionContext}
import io.getquill.monad.ScalaFutureIOMonad
import io.getquill.util.{ContextLogger, LoadConfig}

import scala.concurrent.{ExecutionContext, Future}

class CassandraAsyncContext[N <: NamingStrategy](
  val naming:                     N,
  val cluster:                    Cluster,
  val keyspace:                   String,
  val preparedStatementCacheSize: Long
)
  extends CassandraSessionContext[N]
  with ScalaFutureIOMonad {

  def this(naming: N, config: CassandraContextConfig) = this(naming, config.cluster, config.keyspace, config.preparedStatementCacheSize)
  def this(naming: N, config: Config) = this(naming, CassandraContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  private val logger = ContextLogger(classOf[CassandraAsyncContext[_]])

  override type RunContext = ExecutionContext
  override type Completed = Unit
  override type Result[T] = Future[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override def complete: Unit = ()

  override protected val effect = new CassandraFutureContextEffect

  override def performIO[T](io: IO[T, _], transactional: Boolean = false)(implicit ec: ExecutionContext): Result[T] = {
    if (transactional) logger.underlying.warn("Cassandra doesn't support transactions, ignoring `io.transactional`")
    super.performIO(io)
  }

  override def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext): Future[List[T]] =
    super.executeQuery(cql, prepare, extractor)

  override def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext): Future[T] =
    super.executeQuerySingle(cql, prepare, extractor)

  override def executeAction[T](cql: String, prepare: Prepare = identityPrepare)(implicit ec: ExecutionContext): Future[Unit] =
    super.executeAction(cql, prepare)

  override def executeBatchAction(groups: List[BatchGroup])(implicit ec: ExecutionContext): Future[Unit] =
    super.executeBatchAction(groups)

}
