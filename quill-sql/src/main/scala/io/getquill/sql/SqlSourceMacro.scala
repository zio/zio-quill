package io.getquill.sql

import scala.reflect.macros.whitebox.Context

import SqlQueryShow.sqlQueryShow
import io.getquill.Queryable
import io.getquill.attach.TypeAttachment
import io.getquill.lifting.Unlifting
import io.getquill.norm.NormalizationMacro
import io.getquill.util.Show.Shower

class SqlSourceMacro(val c: Context)
    extends NormalizationMacro
    with TypeAttachment
    with Unlifting {
  import c.universe._

  import SqlQueryShow._

  def run[R, T](q: Expr[Queryable[T]])(implicit r: WeakTypeTag[R], t: WeakTypeTag[T]) = {
    val d = c.WeakTypeTag(c.prefix.tree.tpe)
    val NormalizedQuery(query, extractor) = normalize(q.tree)(d, r, t)
    val sql = SqlQuery(query).show
    c.echo(c.enclosingPosition, sql)
    q"${c.prefix}.run[$t]($sql, $extractor)"
  }
}
