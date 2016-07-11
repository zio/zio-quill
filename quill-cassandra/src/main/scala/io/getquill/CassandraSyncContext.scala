package io.getquill

import scala.annotation.tailrec
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import io.getquill.context.BindedStatementBuilder
import com.typesafe.config.Config
import io.getquill.util.LoadConfig
import io.getquill.context.cassandra.CassandraContextSession
import scala.collection.JavaConverters._

class CassandraSyncContext[N <: NamingStrategy](config: CassandraContextConfig)
  extends CassandraContextSession[N](config) {

  import CassandraSyncContext._

  def this(config: Config) = this(CassandraContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override protected type QueryResult[T] = List[T]
  override protected type SingleQueryResult[T] = T
  override protected type ActionResult[T] = ResultSet
  override protected type BatchedActionResult[T] = List[ResultSet]
  override protected type Params[T] = List[T]

  def executeQuery[T](cql: String, extractor: Row => T = identity[Row] _, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity): List[T] =
    session.execute(prepare(cql, bind))
      .all.asScala.toList.map(extractor)

  def executeQuerySingle[T](cql: String, extractor: Row => T = identity[Row] _, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity): T =
    handleSingleResult(executeQuery(cql, extractor, bind))

  def executeAction[O](cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity, generated: Option[String] = None, returningExtractor: Row => O = identity[Row] _): ResultSet =
    session.execute(prepare(cql, bind))

  def executeActionBatch[T, O](cql: String, bindParams: T => BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = (_: T) => identity[BindedStatementBuilder[BoundStatement]] _, generated: Option[String] = None, returningExtractor: Row => O = identity[Row] _): ActionApply[T] = {
    val func = { (values: List[T]) =>
      @tailrec
      def run(values: List[T], acc: List[ResultSet]): List[ResultSet] =
        values match {
          case Nil => acc
          case head :: tail =>
            run(tail, acc :+ session.execute(prepare(cql, bindParams(head))))
        }
      run(values, List.empty)
    }
    new ActionApply(func)
  }
}

object CassandraSyncContext {

  class ActionApply[T](f: List[T] => List[ResultSet]) extends Function1[List[T], List[ResultSet]] {
    def apply(params: List[T]) = f(params)
    def apply(param: T) = f(List(param)).head
  }
}
