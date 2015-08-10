package io.getquill.sql

import ActionShow._
import scala.reflect.macros.whitebox.Context
import QueryShow.sqlQueryShow
import io.getquill.impl.Queryable
import io.getquill.util.Messages._
import io.getquill.ast.Ident
import io.getquill.ast.Action
import io.getquill.util.Show.Shower
import io.getquill.source.Encoder
import io.getquill.impl.Actionable
import io.getquill.impl.Parser
import io.getquill.norm.Normalize
import io.getquill.util.InferImplicitValueWithFallback
import io.getquill.source.Encoding
import io.getquill.source.BindVariables
import io.getquill.source.EncodeBindVariables

class ActionMacro(val c: Context) extends Parser {
  import c.universe.{ Ident => _, _ }

  def run[R, S, T](action: Expr[Actionable[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree = {
    val normalizedAction = Normalize(actionExtractor(action.tree))
    val sql = normalizedAction.show
    c.info(sql)
    q"${c.prefix}.execute($sql)"
  }

  def run1[P1, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](q: Expr[P1 => Actionable[T]])(bindings: Expr[Iterable[P1]]): Tree =
    runParametrized[R, S, T](q.tree, bindings)

  def run2[P1, P2, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](q: Expr[(P1, P2) => Actionable[T]])(bindings: Expr[Iterable[(P1, P2)]]): Tree =
    runParametrized[R, S, T](q.tree, bindings)

  private def runParametrized[R, S, T](q: Tree, bindings: Expr[Iterable[Any]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) =
    q match {
      case q"(..$params) => $body" =>
        run[R, S, T](body, params, bindings.tree)
    }

  private def run[R, S, T](action: Tree, params: List[ValDef], bindings: Tree)(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) = {
    val normalizedAction = Normalize(actionExtractor(action))
    val (bindedAction, encode) = EncodeBindVariables.forAction[S](c)(normalizedAction, bindingMap(params))
    val sql = bindedAction.show
    c.info(sql)
    q"""
      ${c.prefix}.execute($sql, $bindings.map(value => $encode))
    """
  }

  private def bindingMap(params: List[ValDef]): Map[Ident, (ValDef, Tree)] =
    (for ((param, index) <- params.zipWithIndex) yield {
      identExtractor(param) -> (param, q"value.${TermName(s"_${index + 1}")}")
    }).toMap
}
