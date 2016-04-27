package io.getquill.sources.sql.mirror

import io.getquill.naming.NamingStrategy
import io.getquill.sources.mirror.Row
import io.getquill.sources.sql.SqlSource
import scala.util.Success
import scala.util.Failure
import io.getquill.sources.mirror.MirrorEncoders
import io.getquill.sources.mirror.MirrorDecoders
import io.getquill.SqlMirrorSourceConfig

class SqlMirrorSource[N <: NamingStrategy](config: SqlMirrorSourceConfig[N])
  extends SqlSource[MirrorDialect, N, Row, Row]
  with MirrorEncoders
  with MirrorDecoders {

  override def close = ()

  type QueryResult[T] = QueryMirror[T]
  type ActionResult[T] = ActionMirror
  type BatchedActionResult[T] = BatchActionMirror
  class ActionApply[T](f: List[T] => BatchActionMirror)
    extends Function1[List[T], BatchActionMirror] {
    def apply(params: List[T]) = f(params)
    def apply(param: T) = {
      val r = f(List(param))
      ActionMirror(r.sql, Row(param), r.generated)
    }
  }

  def probe(sql: String) =
    if (sql.contains("Fail"))
      Failure(new IllegalStateException("The sql contains the 'Fail' keyword'"))
    else
      Success(())

  case class ActionMirror(sql: String, bind: Row, generated: Option[String])

  def execute(sql: String, bind: Row => Row, generated: Option[String] = None) =
    ActionMirror(sql, bind(Row()), generated)

  case class BatchActionMirror(sql: String, bindList: List[Row], generated: Option[String])

  def executeBatch[T](sql: String, bindParams: T => Row => Row, generated: Option[String] = None) = {
    val func = (values: List[T]) =>
      BatchActionMirror(sql, values.map(bindParams).map(_(Row())), generated)
    new ActionApply(func)
  }

  case class QueryMirror[T](sql: String, binds: Row, extractor: Row => T)

  def query[T](sql: String, bind: Row => Row, extractor: Row => T) =
    QueryMirror(sql, bind(Row()), extractor)
}
