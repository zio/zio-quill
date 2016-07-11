package io.getquill

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import com.typesafe.config.Config

import scala.collection.JavaConverters._

import io.getquill.context.BindedStatementBuilder
import io.getquill.context.cassandra.CassandraContextSession
import io.getquill.context.cassandra.util.FutureConversions.toScalaFuture
import monifu.reactive.Observable
import io.getquill.util.LoadConfig

class CassandraStreamContext[N <: NamingStrategy](config: CassandraContextConfig)
  extends CassandraContextSession[N](config) {

  def this(config: Config) = this(CassandraContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override type QueryResult[T] = Observable[T]
  override type SingleQueryResult[T] = Observable[T]
  override type ActionResult[T] = Observable[ResultSet]
  override type BatchedActionResult[T] = Observable[ResultSet]
  override type Params[T] = Observable[T]

  protected def page(rs: ResultSet): Observable[Iterable[Row]] = {
    val available = rs.getAvailableWithoutFetching
    val page = rs.asScala.take(available)

    if (rs.isFullyFetched)
      Observable.unit(page)
    else
      Observable.fromFuture(rs.fetchMoreResults()).map(_ => page)
  }

  def executeQuery[T](cql: String, extractor: Row => T = identity[Row] _, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity): Observable[T] = {
    Observable
      .fromFuture(session.executeAsync(prepare(cql, bind)))
      .flatMap(Observable.fromStateAction((rs: ResultSet) => (page(rs), rs)))
      .flatten
      .takeWhile(_.nonEmpty)
      .flatMap(Observable.fromIterable)
      .map(extractor)
  }

  def executeQuerySingle[T](cql: String, extractor: Row => T = identity[Row] _, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity) =
    executeQuery(cql, extractor, bind)

  def executeAction[O](cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity, generated: Option[String] = None, returningExtractor: Row => O = identity[Row] _): Observable[ResultSet] =
    Observable.fromFuture(session.executeAsync(prepare(cql, bind)))

  def executeActionBatch[T, O](cql: String, bindParams: T => BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = (_: T) => identity[BindedStatementBuilder[BoundStatement]] _, generated: Option[String] = None, returningExtractor: Row => O = identity[Row] _): Observable[T] => Observable[ResultSet] =
    (values: Observable[T]) =>
      values.flatMap { value =>
        Observable.fromFuture(session.executeAsync(prepare(cql, bindParams(value))))
      }
}
