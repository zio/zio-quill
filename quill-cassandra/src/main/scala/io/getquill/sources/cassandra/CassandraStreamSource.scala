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

class CassandraStreamSource[N <: NamingStrategy](config: CassandraSourceConfig[N, CassandraStreamSource[N]])
    extends CassandraSourceSession[N](config) {

  override type QueryResult[T] = Observable[T]
  override type ActionResult[T] = Observable[ResultSet]
  override type BatchedActionResult[T] = Observable[ResultSet]
  override type Params[T] = Observable[T]

  private def logged[T](cql: String)(f: => Observable[T]) =
    for {
      _ <- Observable.apply(logger.info(cql))
      r <- f
    } yield {
      r
    }

  def query[T](cql: String, bind: BoundStatement => BoundStatement, extractor: Row => T): Observable[T] =
    logged(cql) {
      Observable
        .fromFuture(session.executeAsync(prepare(cql, bind)))
        .map(_.iterator)
        .flatMap(Observable.fromIterator(_))
        .map(extractor)
    }

  def execute(cql: String): Observable[ResultSet] =
    logged(cql) {
      Observable.fromFuture(session.executeAsync(prepare(cql)))
    }

  def execute[T](cql: String, bindParams: T => BoundStatement => BoundStatement): Observable[T] => Observable[ResultSet] =
    (values: Observable[T]) =>
      values.flatMap { value =>
        logged(cql) {
          Observable.fromFuture(session.executeAsync(prepare(cql, bindParams(value))))
        }
      }
}
