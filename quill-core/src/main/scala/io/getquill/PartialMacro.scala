package io.getquill

import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Ident
import io.getquill.ast.ParametrizedQuery
import io.getquill.ast.Query
import io.getquill.ast.Ref
import io.getquill.norm.BetaReduction
import io.getquill.ast.Expr
import io.getquill.ast.ParametrizedExpr
import io.getquill.attach.TypeAttachment
import io.getquill.lifting.Lifting
import io.getquill.lifting.Unlifting
import io.getquill.ast.Parametrized

class PartialMacro(val c: Context) extends TypeAttachment with Lifting with Unlifting {
  import c.universe.{ Ident => _, Expr => _, _ }

  def create1[P1, T](f: c.Expr[P1 => T])(implicit p1: WeakTypeTag[P1], t: WeakTypeTag[T]) =
    f.tree match {
      case q"(${ pr1: Ident }) => ${ expr: Expr }" =>
        to[Partial1[P1, T]].attach(ParametrizedExpr(List(pr1), expr): Parametrized)
      case q"(${ pr1: Ident }) => $query" =>
        to[Partial1[P1, T]].attach(attachmentData(query), ParametrizedQuery(List(pr1), attachmentMetadata[Query](query)): Parametrized)
    }

  def create2[P1, P2, T](f: c.Expr[(P1, P2) => T])(implicit p1: WeakTypeTag[P1], p2: WeakTypeTag[P2], t: WeakTypeTag[T]) =
    f.tree match {
      case q"(${ pr1: Ident }, ${ pr2: Ident }) => ${ expr: Expr }" =>
        to[Partial2[P1, P2, T]].attach(ParametrizedExpr(List(pr1, pr2), expr): Parametrized)
      case q"(${ pr1: Ident }, ${ pr2: Ident }) => $query" if (query.tpe <:< weakTypeOf[Queryable[Any]]) =>
        to[Partial2[P1, P2, T]].attach(attachmentData(query), ParametrizedQuery(List(pr1, pr2), attachmentMetadata[Query](query)): Parametrized)
    }

  def apply1[P1, T](pr1: c.Expr[P1])(implicit p1: WeakTypeTag[P1], t: WeakTypeTag[T]) = {
    val actuals = List(pr1.tree).map {
      case q"${ ref: Ref }" => ref
    }
    attachmentMetadata[Parametrized](c.prefix.tree) match {
      case ParametrizedQuery(idents, query) =>
        val reductionMap = idents.zip(actuals).toMap
        to[T].attach(attachmentData(c.prefix.tree), BetaReduction(query)(reductionMap))
      case ParametrizedExpr(idents, expr) =>
        val reductionMap = idents.zip(actuals).toMap
        q"${BetaReduction(expr)(reductionMap)}"
    }
  }

  def apply2[P1, P2, T](pr1: c.Expr[P1], pr2: c.Expr[P2])(implicit p2: WeakTypeTag[P2], p1: WeakTypeTag[P1], t: WeakTypeTag[T]) = {
    val actuals = List(pr1.tree, pr2.tree).map {
      case q"${ ref: Ref }" => ref
    }
    attachmentMetadata[Parametrized](c.prefix.tree) match {
      case ParametrizedQuery(idents, query) =>
        val reductionMap = idents.zip(actuals).toMap
        to[T].attach(attachmentData(c.prefix.tree), BetaReduction(query)(reductionMap))
      case ParametrizedExpr(idents, expr) =>
        val reductionMap = idents.zip(actuals).toMap
        q"${BetaReduction(expr)(reductionMap)}"
    }
  }
}