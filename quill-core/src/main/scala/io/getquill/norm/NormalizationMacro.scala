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

trait NormalizationMacro extends  Parser with SelectNormalization with SelectExtraction {

  val c: Context
  import c.universe.{ Expr => _, Ident => _, _ }

  case class NormalizedQuery(query: Query, extractor: Tree)

  sealed trait SelectValue
  case class SimpleSelectValue(expr: Expr, decoder: Tree) extends SelectValue
  case class CaseClassSelectValue(tpe: Type, params: List[List[SimpleSelectValue]]) extends SelectValue

  def normalize[D: WeakTypeTag, R: WeakTypeTag, T: WeakTypeTag](tree: Tree) = {
    val query = Normalize(queryExtractor(tree))
    normalizedQuery[T, R](ensureFinalMap(query), inferDecoder[R, D](_))
  }

  private def normalizedQuery[T: WeakTypeTag, R: WeakTypeTag](query: Query, inferDecoder: Type => Option[Tree]): NormalizedQuery =
    normalizedQuery[T, R](query, normalizeSelect[T](inferDecoder, mapExpr(query)))

  private def normalizedQuery[T: WeakTypeTag, R: WeakTypeTag](query: Query, values: List[SelectValue]) =
    NormalizedQuery(replaceMapExpr(query, selectTuple(values)), selectExtractor[T, R](values))

  private def selectTuple(values: List[SelectValue]) =
    Tuple(selectExprs(values).flatten)

  private def selectExprs(values: List[SelectValue]) =
    values map {
      case SimpleSelectValue(expr, _)      => List(expr)
      case CaseClassSelectValue(_, params) => params.flatten.map(_.expr)
    }

  private def inferDecoder[R, D](tpe: Type)(implicit r: WeakTypeTag[R], d: WeakTypeTag[D]) = {
      def decoderType[T, R](implicit t: WeakTypeTag[T], r: WeakTypeTag[R]) =
        c.weakTypeTag[Decoder[R, T]]
    InferImplicitValueWithFallback(c)(decoderType(c.WeakTypeTag(tpe), r).tpe, c.prefix.tree)
  }

  private def ensureFinalMap(query: Query): Query =
    query match {
      case FlatMap(q, x, p)    => FlatMap(q, x, ensureFinalMap(p))
      case q: Map              => query
      case q @ Filter(_, x, _) => Map(q, x, x)
      case t: Table            => Map(t, Ident("x"), Ident("x"))
    }

  private def mapExpr(query: Query): Expr =
    query match {
      case FlatMap(q, x, p) => mapExpr(p)
      case Map(q, x, p)     => p
      case other            => c.abort(c.enclosingPosition, s"Query not properly normalized, please submit a bug report. $other")
    }

  private def replaceMapExpr(query: Query, expr: Expr): Query =
    query match {
      case FlatMap(q, x, p) => FlatMap(q, x, replaceMapExpr(p, expr))
      case Map(q, x, p)     => Map(q, x, expr)
      case other            => other
    }
}