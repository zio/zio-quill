package io.getquill.sql

import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Ident
import io.getquill.ast.Ast
import io.getquill.Actionable
import io.getquill.norm.Normalize
import io.getquill.source.EncodeBindVariables
import io.getquill.util.Messages.RichContext
import io.getquill.util.Show._
import io.getquill.quotation.Quotation
import AstShow._
import io.getquill.ast.Action

class ActionMacro(val c: Context) extends Quotation {
  import c.universe.{ Ident => _, _ }

  def run[R, S, T](action: Expr[Actionable[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree = {
    val normalizedAction = Normalize(actionUnliftable(action.tree): Ast)
    val sql = normalizedAction.show
    c.info(sql)
    q"${c.prefix}.execute($sql)"
  }

  def run1[P1, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](action: Expr[P1 => Actionable[T]])(bindings: Expr[Iterable[P1]]): Tree =
    runParametrized[R, S, T](action.tree, bindings)

  def run2[P1, P2, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](action: Expr[(P1, P2) => Actionable[T]])(bindings: Expr[Iterable[(P1, P2)]]): Tree =
    runParametrized[R, S, T](action.tree, bindings)

  private def runParametrized[R, S, T](action: Tree, bindings: Expr[Iterable[Any]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    action match {
      case q"io.qetquill.`package`.unquote($quoted)" =>
        runParametrized[R, S, T](quoted, bindings)
      case q"(..$params) => $body" =>
        run[R, S, T](body, params, bindings.tree)
    }

  private def run[R, S, T](action: Tree, params: List[ValDef], bindings: Tree)(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) = {
    val normalizedAction = Normalize(actionUnliftable(action): Ast).asInstanceOf[Action]
    val (bindedAction, encode) = EncodeBindVariables[S](c)(normalizedAction, bindingMap(params))
    val sql = bindedAction.show
    c.info(sql)
    q"""
      ${c.prefix}.execute($sql, $bindings.map(value => $encode))
    """
  }

  private def bindingMap(params: List[ValDef]): Map[Ident, (ValDef, Tree)] =
    params match {
      case param :: Nil =>
        Map((identUnliftable(param), (param, q"value")))
      case params =>
        (for ((param, index) <- params.zipWithIndex) yield {
          identUnliftable(param) -> (param, q"value.${TermName(s"_${index + 1}")}")
        }).toMap
    }
}
