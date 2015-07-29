package io.getquill.sql

import scala.reflect.macros.whitebox.Context

import SqlQueryShow.sqlQueryShow
import io.getquill.impl.Queryable
import io.getquill.norm.NormalizationMacro
import io.getquill.util.Messages
import io.getquill.util.Show.Shower

class SqlSourceMacro(val c: Context) extends NormalizationMacro with Messages {
  import c.universe._

  def run[R, T](q: Expr[Queryable[T]])(implicit r: WeakTypeTag[R], t: WeakTypeTag[T]) = {
    val (sql, extractor) = interpret[R, T](q)
    info(sql)
    q"${c.prefix}.run[$t]($sql, $extractor)"
  }

  private def interpret[R, T](q: Expr[Queryable[T]])(implicit r: WeakTypeTag[R], t: WeakTypeTag[T]) = {
    val d = c.WeakTypeTag(c.prefix.tree.tpe)
    val NormalizedQuery(query, extractor) = normalize(q.tree)(d, r, t)
    (SqlQuery(query).show, extractor)
  }
}
