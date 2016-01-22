package io.getquill.source.cassandra

import scala.collection.JavaConversions.asScalaIterator
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row

import io.getquill.naming.NamingStrategy
import io.getquill.source.cassandra.util.FutureConversions.toScalaFuture
import monifu.reactive.Observable
import monifu.reactive.Observable.FutureIsObservable

class CassandraStreamSource[N <: NamingStrategy]
    extends CassandraSourceSession[N] {

  override type QueryResult[T] = Observable[T]
  override type ActionResult[T] = Observable[ResultSet]
  override type BatchedActionResult[T] = Observable[List[ResultSet]]

  def query[T](cql: String, bind: BoundStatement => BoundStatement, extractor: Row => T)(implicit ec: ExecutionContext): Observable[T] = {
    logger.info(cql)
    Observable
      .fromFuture(session.executeAsync(prepare(cql, bind)))
      .map(_.iterator)
      .flatMap(Observable.fromIterator(_))
      .map(extractor)
  }

  def execute(cql: String)(implicit ec: ExecutionContext): Observable[ResultSet] = {
    logger.info(cql)
    Observable.fromFuture(session.executeAsync(prepare(cql)))
  }

  def execute(cql: String, bindList: List[BoundStatement => BoundStatement])(implicit ec: ExecutionContext): Observable[List[ResultSet]] = {
    logger.info(cql)
    bindList match {
      case Nil => Future.successful(List())
      case head :: tail =>
        Observable.fromFuture(session.executeAsync(prepare(cql, head))).flatMap { result =>
          execute(cql, tail).map(result +: _)
        }
    }
  }
}
