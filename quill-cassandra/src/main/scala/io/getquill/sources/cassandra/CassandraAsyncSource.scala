package io.getquill.sources.cassandra

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.implicitConversions
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import io.getquill.naming.NamingStrategy
import io.getquill.sources.cassandra.util.FutureConversions.toScalaFuture
import io.getquill.CassandraSourceConfig

class CassandraAsyncSource[N <: NamingStrategy](config: CassandraSourceConfig[N, CassandraAsyncSource[N]])
    extends CassandraSourceSession[N](config) {

  override type QueryResult[T] = Future[List[T]]
  override type ActionResult[T] = Future[ResultSet]
  override type BatchedActionResult[T] = Future[List[ResultSet]]
  override type Params[T] = List[T]

  def query[T](cql: String, bind: BoundStatement => BoundStatement, extractor: Row => T)(implicit ec: ExecutionContext): Future[List[T]] = {
    logger.info(cql)
    session.executeAsync(prepare(cql, bind))
      .map(_.all.toList.map(extractor))
  }

  def execute(cql: String)(implicit ec: ExecutionContext): Future[ResultSet] = {
    logger.info(cql)
    session.executeAsync(prepare(cql))
  }

  def execute[T](cql: String, bindParams: T => BoundStatement => BoundStatement)(implicit ec: ExecutionContext): List[T] => Future[List[ResultSet]] = {
    def run(values: List[T]): Future[List[ResultSet]] =
      values match {
        case Nil => Future.successful(List())
        case head :: tail =>
          logger.info(cql)
          session.executeAsync(prepare(cql, bindParams(head))).flatMap { result =>
            run(tail).map(result +: _)
          }
      }
    run _
  }
}
