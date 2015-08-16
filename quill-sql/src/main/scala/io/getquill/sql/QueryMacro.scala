package io.getquill.sql

import scala.reflect.macros.whitebox.Context

import SqlQueryShow.sqlQueryShow
import io.getquill.impl.Parser
import io.getquill.impl.Queryable
import io.getquill.norm.Normalize
import io.getquill.norm.SelectResultExtraction
import io.getquill.norm.select.SelectFlattening
import io.getquill.source.EncodeBindVariables
import io.getquill.source.Encoding
import io.getquill.util.Messages.RichContext
import io.getquill.util.Show.Shower

class QueryMacro(val c: Context) extends Parser with SelectFlattening with SelectResultExtraction {
  import c.universe.{ Ident => _, _ }

  def run[R, S, T](query: Expr[Queryable[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    run[R, S, T](query.tree, List(), List())

  def run1[P1, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](query: Expr[P1 => Queryable[T]])(p1: Expr[P1]): Tree =
    runParametrized[R, S, T](query.tree, List(p1))

  def run2[P1, P2, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](query: Expr[(P1, P2) => Queryable[T]])(p1: Expr[P1], p2: Expr[P2]): Tree =
    runParametrized[R, S, T](query.tree, List(p1, p2))

  private def runParametrized[R, S, T](query: Tree, bindings: List[Expr[Any]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) =
    query match {
      case q"(..$params) => $body" =>
        run[R, S, T](body, params, bindings)
    }

  private def run[R, S, T](query: Tree, params: List[ValDef], bindings: List[Expr[Any]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) = {
    import io.getquill.ast.ExprShow._
    val normalizedQuery = Normalize(queryExtractor(query))
//    c.info(queryExtractor(query).show)
//    c.info(normalizedQuery.show)
    val (flattenQuery, selectValues) = flattenSelect[T](normalizedQuery, Encoding.inferDecoder[R](c))
    val (bindedQuery, encode) = EncodeBindVariables.forQuery[S](c)(flattenQuery, bindingMap(params, bindings))
    val extractor = selectResultExtractor[T, R](selectValues)
    val sql = SqlQuery(bindedQuery).show
    c.info(sql)
    q"""
      ${c.prefix}.query[$t]($sql, $encode, $extractor)
    """
  }

  private def bindingMap(params: List[ValDef], bindings: List[Expr[Any]]) = {
    (for ((param, binding) <- params.zip(bindings)) yield {
      identExtractor(param) -> (param, binding.tree)
    }).toMap
  }

}
