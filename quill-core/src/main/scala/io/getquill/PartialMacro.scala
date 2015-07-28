package io.getquill

import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Parametrized
import io.getquill.ast.ParametrizedExpr
import io.getquill.ast.ParametrizedQuery
import io.getquill.ast.Query
import io.getquill.ast.Ident
import io.getquill.ast.Expr
import io.getquill.attach.TypeAttachment
import io.getquill.lifting.Lifting
import io.getquill.lifting.Unlifting
import io.getquill.norm.BetaReduction

class PartialMacro(val c: Context) extends TypeAttachment with Lifting with Unlifting {
  import c.universe.{ Ident => _, Expr => _, _ }

  def create1[P1: WeakTypeTag, T: WeakTypeTag](f: c.Expr[P1 => T]) = create[T](f)
  def create2[P1: WeakTypeTag, P2: WeakTypeTag, T: WeakTypeTag](f: c.Expr[(P1, P2) => T]) = create[T](f)

  private def create[T](f: c.Expr[Any])(implicit t: WeakTypeTag[T]) =
    f.tree match {
      case q"(..$inputs) => ${ expr: Expr }" =>
        val args = inputs.map {
          case q"${ ident: Ident }" => ident
        }
        attach[Partial[Expr]](ParametrizedExpr(args, expr): Parametrized)
      case q"(..$inputs) => $query" =>
        val args = inputs.map {
          case q"${ ident: Ident }" => ident
        }
        attach[Partial[T]](ParametrizedQuery(args, detach[Query](query)): Parametrized)
    }

  def applyNamed[T](selection: c.Expr[String])(args: c.Expr[(String, Any)]*)(implicit t: WeakTypeTag[T]): Tree = {
    val map =
      args.map(_.tree).map {
        case q"(${ name: String }, ${ expr: ast.Expr })" => name -> expr
      }.toMap[String, Expr]
    detach[Parametrized](c.prefix.tree) match {
      case ParametrizedQuery(idents, query) =>
        val actuals =
          idents.map { ident =>
            map.getOrElse(ident.name, c.abort(c.enclosingPosition, s"Can't find param ${ident.name}"))
          }
        val reductionMap = idents.zip(actuals).toMap
        attach[T](BetaReduction(query)(reductionMap))
      case ParametrizedExpr(idents, expr) =>
        val actuals =
          idents.map { ident =>
            map.getOrElse(ident.name, c.abort(c.enclosingPosition, s"Can't find param ${ident.name}"))
          }
        val reductionMap = idents.zip(actuals).toMap
        q"${BetaReduction(expr)(reductionMap)}"
    }
  }

  def apply[T](selection: c.Expr[String])(args: c.Expr[Any]*)(implicit t: WeakTypeTag[T]): Tree = {
    val actuals = args.map(_.tree).map {
      case q"${ expr: ast.Expr }" => expr
    }
    detach[Parametrized](c.prefix.tree) match {
      case ParametrizedQuery(idents, query) =>
        val reductionMap = idents.zip(actuals).toMap
        attach[T](BetaReduction(query)(reductionMap))
      case ParametrizedExpr(idents, expr) =>
        val reductionMap = idents.zip(actuals).toMap
        q"${BetaReduction(expr)(reductionMap)}"
    }
  }

  private def partialType[T](implicit t: WeakTypeTag[T]) =
    c.weakTypeTag[Partial[T]]
}
