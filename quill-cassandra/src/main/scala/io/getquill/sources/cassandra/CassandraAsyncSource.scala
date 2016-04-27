package io.getquill.sources.cassandra

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import io.getquill.naming.NamingStrategy
import io.getquill.sources.cassandra.util.FutureConversions.toScalaFuture
import io.getquill.CassandraSourceConfig
import io.getquill.sources.BindedStatementBuilder

class CassandraAsyncSource[N <: NamingStrategy](config: CassandraSourceConfig[N, CassandraAsyncSource[N]])
  extends CassandraSourceSession[N](config) {

  override type QueryResult[T] = Future[List[T]]
  override type ActionResult[T] = Future[ResultSet]
  override type BatchedActionResult[T] = Future[List[ResultSet]]
  override type Params[T] = List[T]

  class ActionApply[T](f: List[T] => BatchedActionResult[T])(implicit ec: ExecutionContext) extends Function1[List[T], BatchedActionResult[T]] {
    def apply(params: List[T]) = f(params)
    def apply(param: T) = f(List(param)).map(_.head)
  }

  def query[T](cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement], extractor: Row => T)(implicit ec: ExecutionContext): Future[List[T]] =
    session.executeAsync(prepare(cql, bind))
      .map(_.all.toList.map(extractor))

  def execute(cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement], generated: Option[String] = None)(implicit ec: ExecutionContext): Future[ResultSet] =
    session.executeAsync(prepare(cql, bind))

  def executeBatch[T](cql: String, bindParams: T => BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement], generated: Option[String] = None)(implicit ec: ExecutionContext): ActionApply[T] = {
    def run(values: List[T]): Future[List[ResultSet]] =
      values match {
        case Nil => Future.successful(List())
        case head :: tail =>
          session.executeAsync(prepare(cql, bindParams(head))).flatMap { result =>
            run(tail).map(result +: _)
          }
      }
    new ActionApply(run _)
  }
}
