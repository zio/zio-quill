package io.getquill.sources.cassandra

import scala.annotation.tailrec
import scala.collection.JavaConversions.asScalaBuffer
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Row
import io.getquill.naming.NamingStrategy
import io.getquill.CassandraSourceConfig
import io.getquill.sources.BindedStatementBuilder

class CassandraSyncSource[N <: NamingStrategy](config: CassandraSourceConfig[N, CassandraSyncSource[N]])
  extends CassandraSourceSession[N](config) {

  override type QueryResult[T] = List[T]
  override type ActionResult[T] = ResultSet
  override type BatchedActionResult[T] = List[ResultSet]
  override type Params[T] = List[T]

  class ActionApply[T](f: List[T] => List[ResultSet]) extends Function1[List[T], List[ResultSet]] {
    def apply(params: List[T]) = f(params)
    def apply(param: T) = f(List(param)).head
  }

  def query[T](cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement], extractor: Row => T): List[T] =
    session.execute(prepare(cql, bind))
      .all.toList.map(extractor)

  def execute(cql: String, bind: BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement], generated: Option[String] = None): ResultSet =
    session.execute(prepare(cql, bind))

  def executeBatch[T](cql: String, bindParams: T => BindedStatementBuilder[BoundStatement] => BindedStatementBuilder[BoundStatement], generated: Option[String] = None): ActionApply[T] = {
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
