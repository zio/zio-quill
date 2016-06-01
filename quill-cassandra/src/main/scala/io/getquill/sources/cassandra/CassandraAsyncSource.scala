package io.getquill.sources.cassandra

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import io.getquill.naming.NamingStrategy
import io.getquill.sources.cassandra.util.FutureConversions.toScalaFuture
import io.getquill.sources.BindedStatementBuilder
import io.getquill.util.LoadConfig
import com.typesafe.config.Config

class CassandraAsyncSource[N <: NamingStrategy](config: CassandraSourceConfig)
  extends CassandraSourceSession[N](config) {

  def this(config: Config) = this(CassandraSourceConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override type QueryResult[T] = Future[List[T]]
  override type SingleQueryResult[T] = Future[T]
  override type ActionResult[T] = Future[ResultSet]
  override type BatchedActionResult[T] = Future[List[ResultSet]]
  override type Params[T] = List[T]

  class ActionApply[T](f: List[T] => BatchedActionResult[T])(implicit ec: ExecutionContext) extends Function1[List[T], BatchedActionResult[T]] {
    def apply(params: List[T]) = f(params)
    def apply(param: T) = f(List(param)).map(_.head)
  }

  def executeQuery[T](cql: String, extractor: Row => T = identity[Row] _, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity)(implicit ec: ExecutionContext): Future[List[T]] =
    session.executeAsync(prepare(cql, bind))
      .map(_.all.toList.map(extractor))

  def executeQuerySingle[T](cql: String, extractor: Row => T = identity[Row] _, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity)(implicit ec: ExecutionContext): Future[T] =
    executeQuery(cql, extractor, bind).map(handleSingleResult)

  def executeAction(cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity, generated: Option[String] = None)(implicit ec: ExecutionContext): Future[ResultSet] =
    session.executeAsync(prepare(cql, bind))

  def executeActionBatch[T](cql: String, bindParams: T => BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = (_: T) => identity[BindedStatementBuilder[BoundStatement]] _, generated: Option[String] = None)(implicit ec: ExecutionContext): ActionApply[T] = {
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
