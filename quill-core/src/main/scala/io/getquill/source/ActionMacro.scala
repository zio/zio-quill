package io.getquill.source

import scala.reflect.macros.whitebox.Context
import io.getquill.ast._
import io.getquill.quotation.IsDynamic
import io.getquill.norm.Normalize

trait ActionMacro {
  this: SourceMacro =>

  val c: Context
  import c.universe.{ Ident => _, _ }

  def runAction[S](action: Ast, params: List[(Ident, Type)])(implicit s: WeakTypeTag[S]): Tree =
    params match {
      case Nil => q"${c.prefix}.execute(${toExecutionTree(action)})"
      case params =>
        val encodedParams = EncodeParams[S](c)(bindingMap(params))
        IsDynamic(action) match {
          case false =>
            val (ast, bindings) = io.getquill.source.BindVariables(Normalize(action), params.map(_._1))
            val bindingNames = bindings.map(_.name)
            q"""
            {
              class Partial {
                def using(values: List[(..${params.map(_._2)})]) =
                  ${c.prefix}.execute(
                    ${toExecutionTree(ast)},
                    values.map(value => $encodedParams($bindingNames)))
              }
              new Partial
            }
            """
        }
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
