package io.getquill.norm

import scala.reflect.macros.whitebox.Context
import io.getquill.encoding.Decoder
import io.getquill.impl.Source
import io.getquill.ast._
import io.getquill.ast.Expr
import io.getquill.ast.ExprShow.exprShow
import io.getquill.util.Show._
import io.getquill.impl.Parser
import io.getquill.util.InferImplicitValueWithFallback
import io.getquill.norm.select.SelectFlattening

trait NormalizationMacro extends Parser with SelectFlattening with SelectResultExtraction {

  val c: Context
  import c.universe.{ Expr => _, Ident => _, _ }

  case class NormalizedQuery(query: Query, extractor: Tree)

  def normalize[D: WeakTypeTag, R: WeakTypeTag, T: WeakTypeTag](tree: Tree) = {
    val (query, selectValues) = flattenSelect[T](Normalize(queryExtractor(tree)), inferDecoder[R, D])
    NormalizedQuery(query, selectResultExtractor[T, R](selectValues))
  }

  private def inferDecoder[R, D](tpe: Type)(implicit r: WeakTypeTag[R], d: WeakTypeTag[D]) = {
    def decoderType[T, R](implicit t: WeakTypeTag[T], r: WeakTypeTag[R]) =
      c.weakTypeTag[Decoder[R, T]]
    InferImplicitValueWithFallback(c)(decoderType(c.WeakTypeTag(tpe), r).tpe, c.prefix.tree)
  }

}