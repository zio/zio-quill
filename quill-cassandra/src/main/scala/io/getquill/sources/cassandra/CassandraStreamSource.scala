package io.getquill.sources.cassandra

import scala.collection.JavaConverters._
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import io.getquill.naming.NamingStrategy
import io.getquill.sources.cassandra.util.FutureConversions.toScalaFuture
import monifu.reactive.Observable
import io.getquill.CassandraSourceConfig
import io.getquill.sources.BindedStatementBuilder

class CassandraStreamSource[N <: NamingStrategy](config: CassandraSourceConfig[N, CassandraStreamSource[N]])
  extends CassandraSourceSession[N](config) {

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

  def query[T](cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement], extractor: Row => T): Observable[T] = {
    Observable
      .fromFuture(session.executeAsync(prepare(cql, bind)))
      .flatMap(Observable.fromStateAction((rs: ResultSet) => (page(rs), rs)))
      .flatten
      .takeWhile(_.nonEmpty)
      .flatMap(Observable.fromIterable)
      .map(extractor)
  }

  def querySingle[T](cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement], extractor: Row => T) = query(cql, bind, extractor)

  def execute(cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement], generated: Option[String] = None): Observable[ResultSet] =
    Observable.fromFuture(session.executeAsync(prepare(cql, bind)))

  def executeBatch[T](cql: String, bindParams: T => BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement], generated: Option[String] = None): Observable[T] => Observable[ResultSet] =
    (values: Observable[T]) =>
      values.flatMap { value =>
        Observable.fromFuture(session.executeAsync(prepare(cql, bindParams(value))))
      }
}
