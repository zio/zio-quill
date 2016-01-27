package io.getquill.sources

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
    val extractor = selectResultExtractor[R](selectValues)
    val encodedParams = EncodeParams[S](c)(bindingMap(params))
    val inputs =
      for ((Ident(param), tpe) <- params) yield {
        q"${TermName(param)}: $tpe"
      }
    if (inputs.isEmpty)
      q"""
        ${c.prefix}.query(
            ${prepare(flattenQuery, params.map(_._1))}._1,
            identity,
            $extractor)
      """
    else
      q"""
      {
        val (sql, bindings: List[io.getquill.ast.Ident]) =
            ${prepare(flattenQuery, params.map(_._1))}

        (..$inputs) =>
          ${c.prefix}.query(
            sql,
            $encodedParams(bindings.map(_.name)),
            $extractor)
      }
      """
  }

  private def bindingMap(params: List[(Ident, Type)]): collection.Map[Ident, (Type, Tree)] =
    (for ((param, tpe) <- params) yield {
      (param, (tpe, q"${TermName(param.name)}"))
    }).toMap
}
