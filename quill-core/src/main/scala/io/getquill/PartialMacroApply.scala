package io.getquill

import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Parametrized
import io.getquill.ast.ParametrizedExpr
import io.getquill.ast.ParametrizedQuery
import io.getquill.attach.TypeAttachment
import io.getquill.lifting.Lifting
import io.getquill.lifting.Unlifting
import io.getquill.norm.BetaReduction

class PartialMacroApply(val c: Context)
    extends TypeAttachment
    with Lifting
    with Unlifting {

  import c.universe._

  def applyNamed[T: WeakTypeTag](selection: c.Expr[String])(args: c.Expr[(String, Any)]*): Tree = {
    val p = detach[Parametrized](c.prefix.tree)
    apply(p, actuals(args, p.params))
  }

  def apply[T: WeakTypeTag](selection: c.Expr[String])(args: c.Expr[Any]*): Tree = {
    val p = detach[Parametrized](c.prefix.tree)
    apply(p, actuals(args))
  }

  private def apply[T: WeakTypeTag](p: Parametrized, actuals: Seq[ast.Expr]): Tree =
    apply(p, p.params.zip(actuals).toMap)

  private def apply[T: WeakTypeTag](p: Parametrized, reductionMap: Map[ast.Ident, ast.Expr]) =
    p match {
      case ParametrizedQuery(idents, query) =>
        attach[T](BetaReduction(query)(reductionMap))
      case ParametrizedExpr(idents, expr) =>
        q"${BetaReduction(expr)(reductionMap)}"
    }

  private def actuals(args: Seq[c.Expr[Any]]) =
    args.map(_.tree).map {
      case q"${ expr: ast.Expr }" => expr
    }

  private def actuals(args: Seq[c.Expr[(String, Any)]], params: List[ast.Ident]) = {
    val map = actualsMap(args)
    params.map { ident =>
      map.getOrElse(ident.name, c.abort(c.enclosingPosition, s"Can't find param ${ident.name}"))
    }
  }

  private def actualsMap(args: Seq[c.Expr[(String, Any)]]) =
    args.map(_.tree).map {
      case q"(${ name: String }, ${ expr: ast.Expr })" => name -> expr
    }.toMap[String, ast.Expr]
}
