package io.getquill

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import com.typesafe.config.Config

import scala.collection.JavaConverters._

import io.getquill.context.cassandra.CassandraSessionContext
import io.getquill.context.cassandra.util.FutureConversions.toScalaFuture
import monifu.reactive.Observable
import io.getquill.util.LoadConfig

class CassandraStreamContext[N <: NamingStrategy](config: CassandraContextConfig)
  extends CassandraSessionContext[N](config) {

  def this(config: Config) = this(CassandraContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override type RunQueryResult[T] = Observable[T]
  override type RunQuerySingleResult[T] = Observable[T]
  override type RunActionResult = Observable[Unit]
  override type RunBatchActionResult = Observable[Unit]

  protected def page(rs: ResultSet): Observable[Iterable[Row]] = {
    val available = rs.getAvailableWithoutFetching
    val page = rs.asScala.take(available)

    if (rs.isFullyFetched)
      Observable.unit(page)
    else
      Observable.fromFuture(rs.fetchMoreResults()).map(_ => page)
  }

  def executeQuery[T](cql: String, prepare: BoundStatement => BoundStatement = identity, extractor: Row => T = identity[Row] _): Observable[T] =
    Observable
      .fromFuture(session.executeAsync(prepare(super.prepare(cql))))
      .flatMap(Observable.fromStateAction((rs: ResultSet) => (page(rs), rs)))
      .flatten
      .takeWhile(_.nonEmpty)
      .flatMap(Observable.fromIterable)
      .map(extractor)

  def executeQuerySingle[T](cql: String, prepare: BoundStatement => BoundStatement = identity, extractor: Row => T = identity[Row] _): Observable[T] =
    executeQuery(cql, prepare, extractor)

  def executeAction[T](cql: String, prepare: BoundStatement => BoundStatement = identity): Observable[Unit] =
    Observable.fromFuture(session.executeAsync(prepare(super.prepare(cql)))).map(_ => ())

  def executeBatchAction(groups: List[BatchGroup]): Observable[Unit] =
    Observable.fromIterable(groups).flatMap {
      case BatchGroup(cql, prepare) =>
        Observable.fromIterable(prepare)
          .flatMap(executeAction(cql, _))
          .map(_ => ())
    }
}
