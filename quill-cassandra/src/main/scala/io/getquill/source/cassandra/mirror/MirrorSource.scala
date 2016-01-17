package io.getquill.source.cassandra.mirror

import io.getquill.naming.Literal
import io.getquill.naming.NamingStrategy
import io.getquill.source.cassandra.CassandraSource
import io.getquill.source.mirror.Row
import io.getquill.source.mirror.MirrorEncoders
import io.getquill.source.mirror.MirrorDecoders
import scala.util.Failure
import scala.util.Success
import com.datastax.driver.core.ConsistencyLevel

object mirrorSource
    extends CassandraSource[Literal, Row, Row]
    with MirrorEncoders
    with MirrorDecoders {

  def withConsistencyLevel(level: ConsistencyLevel) = this

  override def close = ()

  override def probe(cql: String) =
    if (cql.contains("fail"))
      Failure(new IllegalStateException)
    else
      Success(())

  type QueryResult[T] = QueryMirror[T]
  type ActionResult[T] = ActionMirror
  type BatchedActionResult[T] = BatchActionMirror

  case class ActionMirror(cql: String)

  def execute(cql: String) =
    ActionMirror(cql)

  case class BatchActionMirror(cql: String, bindList: List[Row])

  def execute(cql: String, bindList: List[Row => Row]) =
    BatchActionMirror(cql, bindList.map(_(Row())))

  case class QueryMirror[T](cql: String, binds: Row, extractor: Row => T)

  def query[T](cql: String, bind: Row => Row, extractor: Row => T) =
    QueryMirror(cql, bind(Row()), extractor)
}
