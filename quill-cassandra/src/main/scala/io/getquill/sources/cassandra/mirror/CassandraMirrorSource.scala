package io.getquill.sources.cassandra.mirror

import io.getquill.naming.Literal
import io.getquill.naming.NamingStrategy
import io.getquill.sources.cassandra.CassandraSource
import io.getquill.sources.mirror.Row
import io.getquill.sources.mirror.MirrorEncoders
import io.getquill.sources.mirror.MirrorDecoders
import scala.util.Failure
import scala.util.Success
import com.datastax.driver.core.ConsistencyLevel
import io.getquill.sources.SourceConfig
import io.getquill.CassandraMirrorSourceConfig

class CassandraMirrorSource(config: CassandraMirrorSourceConfig)
    extends CassandraSource[Literal, Row, Row]
    with MirrorEncoders
    with MirrorDecoders {

  override type QueryResult[T] = QueryMirror[T]
  override type ActionResult[T] = ActionMirror
  override type BatchedActionResult[T] = BatchActionMirror
  override type Params[T] = List[T]

  override def close = ()

  override def probe(cql: String) =
    if (cql.contains("fail"))
      Failure(new IllegalStateException)
    else
      Success(())

  case class ActionMirror(cql: String)

  def execute(cql: String) =
    ActionMirror(cql)

  case class BatchActionMirror(cql: String, bindList: List[Row])

  def execute[T](sql: String, bindParams: T => Row => Row) =
    (values: List[T]) =>
      BatchActionMirror(sql, values.map(bindParams).map(_(Row())))

  case class QueryMirror[T](cql: String, binds: Row, extractor: Row => T)

  def query[T](cql: String, bind: Row => Row, extractor: Row => T) =
    QueryMirror(cql, bind(Row()), extractor)
}
