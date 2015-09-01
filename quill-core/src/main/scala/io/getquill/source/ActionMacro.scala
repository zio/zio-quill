package io.getquill.source

import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Action
import io.getquill.ast.Ident

trait ActionMacro {
  this: SourceMacro =>

  val c: Context
  import c.universe.{ Ident => _, _ }

  def runAction(action: Action): Tree =
    q"${c.prefix}.execute(${toExecutionTree(action)})"

  def runAction[S](action: Action, params: List[(Ident, Type)])(implicit s: WeakTypeTag[S]): Tree = {
    val (bindedAction, encode) = EncodeBindVariables[S](c)(action, bindingMap(params))
    q"""
    {
      class Partial {
        def using(bindings: List[(..${params.map(_._2)})]) =
          ${c.prefix}.execute(
            ${toExecutionTree(bindedAction)},
            bindings.map(value => $encode))
      }
      new Partial
    }
    """
  }

  private def bindingMap(params: List[(Ident, Type)]): collection.Map[Ident, (Type, Tree)] =
    params match {
      case (param, tpe) :: Nil =>
        collection.Map((param, (tpe, q"value")))
      case params =>
        (for (((param, tpe), index) <- params.zipWithIndex) yield {
          (param, (tpe, q"value.${TermName(s"_${index + 1}")}"))
        }).toMap
    }
}
