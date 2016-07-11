package io.getquill

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import io.getquill.context.cassandra.util.FutureConversions.toScalaFuture
import io.getquill.context.BindedStatementBuilder
import io.getquill.util.LoadConfig
import com.typesafe.config.Config
import io.getquill.context.cassandra.CassandraContextSession
import scala.collection.JavaConverters._

class CassandraAsyncContext[N <: NamingStrategy](config: CassandraContextConfig)
  extends CassandraContextSession[N](config) {

  import CassandraAsyncContext._

  def this(config: Config) = this(CassandraContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override protected type QueryResult[T] = Future[List[T]]
  override protected type SingleQueryResult[T] = Future[T]
  override protected type ActionResult[T] = Future[ResultSet]
  override protected type BatchedActionResult[T] = Future[List[ResultSet]]
  override protected type Params[T] = List[T]

  def executeQuery[T](cql: String, extractor: Row => T = identity[Row] _, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity)(implicit ec: ExecutionContext): Future[List[T]] =
    session.executeAsync(prepare(cql, bind))
      .map(_.all.asScala.toList.map(extractor))

  def executeQuerySingle[T](cql: String, extractor: Row => T = identity[Row] _, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity)(implicit ec: ExecutionContext): Future[T] =
    executeQuery(cql, extractor, bind).map(handleSingleResult)

  def executeAction[O](cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity, generated: Option[String] = None, returningExtractor: Row => O = identity[Row] _)(implicit ec: ExecutionContext): Future[ResultSet] =
    session.executeAsync(prepare(cql, bind))

  def executeActionBatch[T, O](cql: String, bindParams: T => BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = (_: T) => identity[BindedStatementBuilder[BoundStatement]] _, generated: Option[String] = None, returningExtractor: Row => O = identity[Row] _)(implicit ec: ExecutionContext): ActionApply[T] = {
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

object CassandraAsyncContext {
  class ActionApply[T](f: List[T] => Future[List[ResultSet]])(implicit ec: ExecutionContext) extends Function1[List[T], Future[List[ResultSet]]] {
    def apply(params: List[T]) = f(params)
    def apply(param: T) = f(List(param)).map(_.head)
  }
}
