package io.getquill.source

import scala.reflect.macros.whitebox.Context
import io.getquill.ast._
import io.getquill.norm.select.SelectFlattening
import io.getquill.norm.select.SelectResultExtraction
import io.getquill.norm.Normalize

trait QueryMacro extends SelectFlattening with SelectResultExtraction {
  this: SourceMacro =>

  val c: Context
  import c.universe.{ Ident => _, _ }

  def runQuery[R, S, T](ast: Ast, params: List[(Ident, Type)])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree = {
    val query =
      Normalize(ast) match {
        case q: Query => q
        case q        => Map(q, Ident("x"), Ident("x"))
      }
    val (flattenQuery, selectValues) = flattenSelect[T](query, Encoding.inferDecoder[R](c))
    val (bindedQuery, encode) = EncodeBindVariables[S](c)(flattenQuery, bindingMap(params))
    val extractor = selectResultExtractor[R](selectValues)
    val inputs =
      for ((Ident(param), tpe) <- params) yield {
        q"${TermName(param)}: $tpe"
      }
    if (inputs.isEmpty)
      q"""
        ${c.prefix}.query(
            ${toExecutionTree(bindedQuery)},
            $encode,
            $extractor)
      """
    else
      q"""
      {
        class Partial {
          def using(..$inputs) =
            ${c.prefix}.query(
              ${toExecutionTree(bindedQuery)},
              $encode,
              $extractor)
        }
        new Partial
      }
      """
  }

  private def bindingMap(params: List[(Ident, Type)]): collection.Map[Ident, (Type, Tree)] =
    (for ((param, tpe) <- params) yield {
      (param, (tpe, q"${TermName(param.name)}"))
    }).toMap
}
