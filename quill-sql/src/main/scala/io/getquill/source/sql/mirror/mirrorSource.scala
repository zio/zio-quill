package io.getquill.source.sql.mirror

import io.getquill.naming.{Literal, NamingStrategy}
import io.getquill.source.mirror.Row
import io.getquill.source.sql.SqlSource
import io.getquill.source.sql.idiom.FallbackDialect
import scala.util.Success
import scala.util.Failure

trait MirrorSourceTemplate[N <: NamingStrategy] extends SqlSource[MirrorDialect.type, N, Row, Row]
    with MirrorEncoders
    with MirrorDecoders {

  def probe(sql: String) =
    if (sql.contains("Fail"))
      Failure(new IllegalStateException("The sql contains the 'Fail' keyword'"))
    else
      Success(())

  case class ActionMirror(sql: String)

  def execute(sql: String) =
    ActionMirror(sql)

  case class BatchActionMirror(sql: String, bindList: List[Row])

  def execute(sql: String, bindList: List[Row => Row]) =
    BatchActionMirror(sql, bindList.map(_(Row())))

  case class QueryMirror[T](sql: String, binds: Row, extractor: Row => T)

  def query[T](sql: String, bind: Row => Row, extractor: Row => T) =
    QueryMirror(sql, bind(Row()), extractor)
}

object mirrorSource extends MirrorSourceTemplate[Literal]
