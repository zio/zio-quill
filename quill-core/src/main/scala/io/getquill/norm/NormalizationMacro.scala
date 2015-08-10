package io.getquill.norm

import scala.reflect.macros.whitebox.Context
import io.getquill.source.Decoder
import io.getquill.source.Source
import io.getquill.ast._
import io.getquill.ast.Expr
import io.getquill.ast.ExprShow.exprShow
import io.getquill.util.Show._
import io.getquill.impl.Parser
import io.getquill.util.InferImplicitValueWithFallback
import io.getquill.norm.select.SelectFlattening
import io.getquill.source.Encoding

trait NormalizationMacro extends Parser with SelectFlattening with SelectResultExtraction {

  val c: Context
  import c.universe.{ Expr => _, Ident => _, _ }

  case class NormalizedQuery(query: Query, extractor: Tree)

  def normalize[R: WeakTypeTag, T: WeakTypeTag](tree: Tree) = {
    val (query, selectValues) = flattenSelect[T](Normalize(queryExtractor(tree)), Encoding.inferDecoder[R](c))
    NormalizedQuery(query, selectResultExtractor[T, R](selectValues))
  }
}