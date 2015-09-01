package io.getquill.source

import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Ident
import io.getquill.ast.Query
import io.getquill.norm.select.SelectFlattening
import io.getquill.norm.select.SelectResultExtraction

trait QueryMacro extends SelectFlattening with SelectResultExtraction {
  this: SourceMacro =>

  val c: Context
  import c.universe.{ Ident => _, _ }

  def runQuery[R, S, T](query: Query, params: List[(Ident, Type)])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree = {
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
