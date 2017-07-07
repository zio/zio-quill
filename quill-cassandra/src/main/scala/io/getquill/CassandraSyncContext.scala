package io.getquill

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Row
import com.typesafe.config.Config
import io.getquill.util.{ ContextLogger, LoadConfig }
import io.getquill.context.cassandra.CassandraSessionContext
import scala.collection.JavaConverters._
import com.datastax.driver.core.Cluster

class CassandraSyncContext[N <: NamingStrategy](
  cluster:                    Cluster,
  keyspace:                   String,
  preparedStatementCacheSize: Long
)
  extends CassandraSessionContext[N](cluster, keyspace, preparedStatementCacheSize) {

  def this(config: CassandraContextConfig) = this(config.cluster, config.keyspace, config.preparedStatementCacheSize)
  def this(config: Config) = this(CassandraContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  private val logger = ContextLogger(classOf[CassandraSyncContext[_]])

  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Unit
  override type RunBatchActionResult = Unit

  def executeQuery[T](cql: String, prepare: BoundStatement => (List[Any], BoundStatement) = row => (Nil, row), extractor: Row => T = identity[Row] _): List[T] = {
    val (params, bs) = prepare(super.prepare(cql))
    logger.logQuery(cql, params)
    session.execute(bs)
      .all.asScala.toList.map(extractor)
  }

  def executeQuerySingle[T](cql: String, prepare: BoundStatement => (List[Any], BoundStatement) = row => (Nil, row), extractor: Row => T = identity[Row] _): T =
    handleSingleResult(executeQuery(cql, prepare, extractor))

  def executeAction[T](cql: String, prepare: BoundStatement => (List[Any], BoundStatement) = row => (Nil, row)): Unit = {
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
