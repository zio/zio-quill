package io.getquill.source.cassandra

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.implicitConversions

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row

import io.getquill.naming.NamingStrategy
import io.getquill.source.cassandra.util.FutureConversions.toScalaFuture

class CassandraAsyncSource[N <: NamingStrategy]
    extends CassandraSourceSession[N] {

  override type QueryResult[T] = Future[List[T]]
  override type ActionResult[T] = Future[ResultSet]
  override type BatchedActionResult[T] = Future[List[ResultSet]]

  def query[T](cql: String, bind: BoundStatement => BoundStatement, extractor: Row => T)(implicit ec: ExecutionContext): Future[List[T]] = {
    logger.info(cql)
    session.executeAsync(prepare(cql, bind))
      .map(_.all.toList.map(extractor))
  }

  def execute(cql: String)(implicit ec: ExecutionContext): Future[ResultSet] = {
    logger.info(cql)
    session.executeAsync(prepare(cql))
  }

  def execute(cql: String, bindList: List[BoundStatement => BoundStatement])(implicit ec: ExecutionContext): Future[List[ResultSet]] = {
    logger.info(cql)
    bindList match {
      case Nil => Future.successful(List())
      case head :: tail =>
        session.executeAsync(prepare(cql, head)).flatMap { result =>
          execute(cql, tail).map(result +: _)
        }
    }
  }
}
