package io.getquill.source

import scala.reflect.macros.whitebox.Context

import io.getquill.Actionable
import io.getquill.ast.Action
import io.getquill.ast.Ast
import io.getquill.ast.Function
import io.getquill.ast.Ident
import io.getquill.norm.Normalize
import io.getquill.quotation.Quotation

trait ActionMacro extends Quotation {

  val c: Context
  import c.universe.{ Ident => _, Function => _, _ }

  protected def toExecutionTree(ast: Ast): Tree

  def run[R, S, T](action: Expr[Actionable[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree = {
    val normalizedAction = Normalize(actionParser(action.tree): Ast)
    q"${c.prefix}.execute(${toExecutionTree(normalizedAction)})"
  }

  def run1[P1, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](action: Expr[P1 => Actionable[T]])(bindings: Expr[Iterable[P1]])(implicit p1: WeakTypeTag[P1]): Tree =
    run[R, S, T](action.tree, List(p1.tpe), bindings.tree)

  def run2[P1, P2, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](action: Expr[(P1, P2) => Actionable[T]])(bindings: Expr[Iterable[(P1, P2)]])(implicit p1: WeakTypeTag[P1], p2: WeakTypeTag[P2]): Tree =
    run[R, S, T](action.tree, List(p1.tpe, p2.tpe), bindings.tree)

  private def run[R, S, T](tree: Tree, types: List[Type], bindings: Tree)(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) = {
    val (params, action) =
      Normalize(astParser(tree)) match {
        case Function(params, action: Action) => (params, action)
        case other                            => throw new IllegalStateException(s"Invalid action $tree.")
      }
    val (bindedAction, encode) = EncodeBindVariables[S](c)(action, bindingMap(params, types))
    q"""
      ${c.prefix}.execute(${toExecutionTree(bindedAction)}, $bindings.map(value => $encode))
    """
  }

  private def bindingMap(params: List[Ident], types: List[Type]): Map[Ident, (Type, Tree)] =
    params.zip(types) match {
      case (param, tpe) :: Nil =>
        Map((param, (tpe, q"value")))
      case params =>
        (for (((param, tpe), index) <- params.zipWithIndex) yield {
          param -> (tpe, q"value.${TermName(s"_${index + 1}")}")
        }).toMap
    }
}
