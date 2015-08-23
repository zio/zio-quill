package io.getquill.sql

import scala.reflect.macros.whitebox.Context
import SqlQueryShow.sqlQueryShow
import io.getquill.Queryable
import io.getquill.norm.Normalize
import io.getquill.norm.SelectResultExtraction
import io.getquill.norm.select.SelectFlattening
import io.getquill.source.EncodeBindVariables
import io.getquill.source.Encoding
import io.getquill.util.Messages.RichContext
import io.getquill.util.Show.Shower
import io.getquill.quotation.Quotation
import io.getquill.ast.Query
import io.getquill.ast.Function
import io.getquill.ast.Ident

class QueryMacro(val c: Context) extends Quotation with SelectFlattening with SelectResultExtraction {
  import c.universe.{ Ident => _, Function => _, _ }

  def run[R, S, T](query: Expr[Queryable[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    run[R, S, T](query.tree, List())

  def run1[P1, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](query: Expr[P1 => Queryable[T]])(p1: Expr[P1]): Tree =
    run[R, S, T](query.tree, List(p1))

  def run2[P1, P2, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](query: Expr[(P1, P2) => Queryable[T]])(p1: Expr[P1], p2: Expr[P2]): Tree =
    run[R, S, T](query.tree, List(p1, p2))

  private def run[R, S, T](tree: Tree, bindings: List[Expr[Any]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) = {
    val (params, query) =
      Normalize(astParser(tree)) match {
        case Function(params, query: Query) => (params, query)
        case q: Query                       => (List(), q)
        case other                          => c.fail(s"Invalid query $other")
      }
    val (flattenQuery, selectValues) = flattenSelect[T](query, Encoding.inferDecoder[R](c))
    val (bindedQuery, encode) = EncodeBindVariables[S](c)(flattenQuery, bindingMap(params, bindings))
    val extractor = selectResultExtractor[T, R](selectValues)
    val sql = SqlQuery(bindedQuery).show
    c.info(sql)
    q"""
      ${c.prefix}.query[$t]($sql, $encode, $extractor)
    """
  }

  private def bindingMap(params: List[Ident], bindings: List[Expr[Any]]) = {
    (for ((param, binding) <- params.zip(bindings)) yield {
      param -> (binding.actualType.erasure, binding.tree)
    }).toMap
  }

}
