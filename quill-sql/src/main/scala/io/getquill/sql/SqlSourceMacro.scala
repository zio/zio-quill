package io.getquill.sql

import io.getquill.norm.NormalizationMacro
import io.getquill.attach.TypeAttachment
import io.getquill.lifting.Unlifting
import scala.reflect.macros.whitebox.Context
import io.getquill.Queryable

class SqlSourceMacro(val c: Context)
    extends NormalizationMacro
    with TypeAttachment
    with Unlifting {
  import c.universe._

  def run[T](q: Expr[Queryable[T]])(implicit t: WeakTypeTag[T]) = {
    val NormalizedQuery(query, extractor) = normalize[T](q.tree)
    val sql = SqlQuery(query)
    q"${q.toString}"
  }
}
