package io.getquill.sources.cassandra.mirror

import io.getquill.naming.Literal
import io.getquill.sources.cassandra.CassandraSource
import io.getquill.sources.mirror.Row
import io.getquill.sources.mirror.MirrorEncoders
import io.getquill.sources.mirror.MirrorDecoders
import scala.util.Failure
import scala.util.Success
import io.getquill.CassandraMirrorSourceConfig

class CassandraMirrorSource(config: CassandraMirrorSourceConfig)
  extends CassandraSource[Literal, Row, Row]
  with MirrorEncoders
  with MirrorDecoders {

  override type QueryResult[T] = QueryMirror[T]
  override type ActionResult[T] = ActionMirror
  override type BatchedActionResult[T] = BatchActionMirror
  override type Params[T] = List[T]

  class ActionApply[T](f: Params[T] => BatchActionMirror) extends (Params[T] => BatchActionMirror) {
    def apply(params: Params[T]) = f(params)
    def apply(param: T) = ActionMirror(f(List(param)).cql)
  }

  override def close = ()

  override def probe(cql: String) =
    if (cql.contains("fail"))
      Failure(new IllegalStateException)
    else
      Success(())

  case class ActionMirror(cql: String)

  def execute(cql: String, generated: Option[String]) =
    ActionMirror(cql)

  case class BatchActionMirror(cql: String, bindList: List[Row])

  def execute[T](cql: String, bindParams: T => Row => Row, generated: Option[String]) = {
    val f = (values: List[T]) =>
      BatchActionMirror(cql, values.map(bindParams).map(_(Row())))
    new ActionApply[T](f)
  }

  case class QueryMirror[T](cql: String, binds: Row, extractor: Row => T)

  def query[T](cql: String, bind: Row => Row, extractor: Row => T) =
    QueryMirror(cql, bind(Row()), extractor)
}
