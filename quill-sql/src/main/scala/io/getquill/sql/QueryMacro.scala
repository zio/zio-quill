package io.getquill.sql

import scala.reflect.macros.whitebox.Context
import QueryShow.sqlQueryShow
import io.getquill.impl.Queryable
import io.getquill.norm.Normalize
import io.getquill.util.Messages._
import io.getquill.util.Show.Shower
import io.getquill.source.Encoder
import io.getquill.util.InferImplicitValueWithFallback
import io.getquill.source.Encoding
import io.getquill.norm.select.SelectFlattening
import io.getquill.norm.SelectResultExtraction
import io.getquill.impl.Parser
import io.getquill.ast.Query
import io.getquill.ast.Ident
import io.getquill.source.EncodeBindVariables

class QueryMacro(val c: Context) extends Parser with SelectFlattening with SelectResultExtraction {
  import c.universe.{ Ident => _, _ }

  def run[R, S, T](q: Expr[Queryable[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    run[R, S, T](q.tree, List(), List())

  def run1[P1, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](q: Expr[P1 => Queryable[T]])(p1: Expr[P1]): Tree =
    runParametrized[R, S, T](q.tree, List(p1))

  def run2[P1, P2, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](q: Expr[(P1, P2) => Queryable[T]])(p1: Expr[P1], p2: Expr[P2]): Tree =
    runParametrized[R, S, T](q.tree, List(p1, p2))

  private def runParametrized[R, S, T](q: Tree, bindings: List[Expr[Any]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) =
    q match {
      case q"(..$params) => $body" =>
        run[R, S, T](body, params, bindings)
    }

  private def run[R, S, T](q: Tree, params: List[ValDef], bindings: List[Expr[Any]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) = {
    val bindingMap =
      (for ((param, binding) <- params.zip(bindings)) yield {
        identExtractor(param) -> (param, binding.tree)
      }).toMap
    val (query, selectValues) = flattenSelect[T](Normalize(queryExtractor(q)), Encoding.inferDecoder[R](c))
    val extractor = selectResultExtractor[T, R](selectValues)
    val (bindedQuery, encode) = EncodeBindVariables.query[S](c)(query, bindingMap)
    val sql = SqlQuery(bindedQuery).show
    c.info(sql)
    q"""
      ${c.prefix}.query[$t]($sql, $encode, $extractor)
    """
  }

}
