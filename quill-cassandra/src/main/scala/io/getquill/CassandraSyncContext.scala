package io.getquill

import com.typesafe.config.Config
import io.getquill.util.{ ContextLogger, LoadConfig }
import io.getquill.context.cassandra.CassandraSessionContext
import scala.collection.JavaConverters._
import com.datastax.driver.core.Cluster
import io.getquill.monad.SyncIOMonad

class CassandraSyncContext[N <: NamingStrategy](
  naming:                     N,
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends CassandraSessionContext[N](naming, cluster, keyspace, preparedStatementCacheSize)
  with SyncIOMonad {

  def this(naming: N, config: CassandraContextConfig) = this(naming, config.cluster, config.keyspace, config.preparedStatementCacheSize)
  def this(naming: N, config: Config) = this(naming, CassandraContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  private val logger = ContextLogger(classOf[CassandraSyncContext[_]])

  override type Result[T] = T
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Unit
  override type RunBatchActionResult = Unit

  override def performIO[T](io: IO[T, _], transactional: Boolean = false): Result[T] = {
    if (transactional) logger.underlying.warn("Cassandra doesn't support transactions, ignoring `io.transactional`")
    super.performIO(io)
  }

  def executeQuery[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): List[T] = {
    val (params, bs) = prepare(super.prepare(cql))
    logger.logQuery(cql, params)
    session.execute(bs)
      .all.asScala.toList.map(extractor)
  }

  def executeQuerySingle[T](cql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): T =
    handleSingleResult(executeQuery(cql, prepare, extractor))

  def executeAction[T](cql: String, prepare: Prepare = identityPrepare): Unit = {
    val (params, bs) = prepare(super.prepare(cql))
    logger.logQuery(cql, params)
    session.execute(bs)
    ()
  }

  def executeBatchAction(groups: List[BatchGroup]): Unit =
    groups.foreach {
      case BatchGroup(cql, prepare) =>
        prepare.foreach(executeAction(cql, _))
    }
}
