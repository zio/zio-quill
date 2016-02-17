package io.getquill.sources.cassandra

import scala.collection.JavaConversions.asScalaIterator
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import io.getquill.naming.NamingStrategy
import io.getquill.sources.cassandra.util.FutureConversions.toScalaFuture
import monifu.reactive.Observable
import monifu.reactive.Observable.FutureIsObservable
import io.getquill.CassandraSourceConfig
import io.getquill.sources.BindedStatementBuilder

class CassandraStreamSource[N <: NamingStrategy](config: CassandraSourceConfig[N, CassandraStreamSource[N]])
  extends CassandraSourceSession[N](config) {

  override type QueryResult[T] = Observable[T]
  override type ActionResult[T] = Observable[ResultSet]
  override type BatchedActionResult[T] = Observable[ResultSet]
  override type Params[T] = Observable[T]

  def query[T](cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement], extractor: Row => T): Observable[T] = {
    Observable
      .fromFuture(session.executeAsync(prepare(cql, bind)))
      .map(_.iterator)
      .flatMap(Observable.fromIterator(_))
      .map(extractor)
  }

  def execute(cql: String): Observable[ResultSet] =
    Observable.fromFuture(session.executeAsync(prepare(cql)))

  def execute[T](cql: String, bindParams: T => BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement]): Observable[T] => Observable[ResultSet] =
    (values: Observable[T]) =>
      values.flatMap { value =>
        Observable.fromFuture(session.executeAsync(prepare(cql, bindParams(value))))
      }
}
