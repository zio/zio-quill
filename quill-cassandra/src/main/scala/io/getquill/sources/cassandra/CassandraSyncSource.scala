package io.getquill.sources.cassandra

import scala.annotation.tailrec
import scala.collection.JavaConversions.asScalaBuffer
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import io.getquill.naming.NamingStrategy
import io.getquill.sources.BindedStatementBuilder
import com.typesafe.config.Config
import io.getquill.util.LoadConfig

class CassandraSyncSource[N <: NamingStrategy](config: CassandraSourceConfig)
  extends CassandraSourceSession[N](config) {

  def this(config: Config) = this(CassandraSourceConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override type QueryResult[T] = List[T]
  override type SingleQueryResult[T] = T
  override type ActionResult[T] = ResultSet
  override type BatchedActionResult[T] = List[ResultSet]
  override type Params[T] = List[T]

  class ActionApply[T](f: List[T] => List[ResultSet]) extends Function1[List[T], List[ResultSet]] {
    def apply(params: List[T]) = f(params)
    def apply(param: T) = f(List(param)).head
  }

  def executeQuery[T](cql: String, extractor: Row => T = identity[Row] _, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity): List[T] =
    session.execute(prepare(cql, bind))
      .all.toList.map(extractor)

  def executeQuerySingle[T](cql: String, extractor: Row => T = identity[Row] _, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity): T =
    handleSingleResult(executeQuery(cql, extractor, bind))

  def executeAction(cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = identity, generated: Option[String] = None): ResultSet =
    session.execute(prepare(cql, bind))

  def executeActionBatch[T](cql: String, bindParams: T => BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement] = (_: T) => identity[BindedStatementBuilder[BoundStatement]] _, generated: Option[String] = None): ActionApply[T] = {
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
