package io.getquill.context

import scala.reflect.macros.whitebox.{ Context => MacroContext }

import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.norm.Normalize
import io.getquill.norm.select.SelectFlattening
import io.getquill.norm.select.SelectResultExtraction

class QueryMacro(val c: MacroContext) extends ContextMacro with SelectFlattening with SelectResultExtraction {
  import c.universe.{ Ident => _, _ }

  def runQuery[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, "executeQuery")

  def runQuerySingle[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, "executeQuerySingle")

  private def expandQuery[T](quoted: Tree, method: String)(implicit t: WeakTypeTag[T]) = {
    val ast = extractAst(quoted)
    val query = Normalize(ast) match {
      case q: Query => q
      case q        => Map(q, Ident("x"), Ident("x"))
    }
    val (flattenAst, selectValues) = flattenSelect(query, t.tpe, Encoding.inferDecoder(c))
    val extractor = selectResultExtractor(selectValues)
    q"""
      val expanded = ${expand(flattenAst)}
      ${c.prefix}.${TermName(method)}(
        expanded.string,
        expanded.prepare,
        $extractor
      )  
    """
  }
}
