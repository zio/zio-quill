package io.getquill

import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Parametrized
import io.getquill.ast.ParametrizedExpr
import io.getquill.ast.Query
import io.getquill.attach.TypeAttachment
import io.getquill.lifting.Lifting
import io.getquill.lifting.Unlifting
import io.getquill.norm.BetaReduction
import io.getquill.norm.NormalizationMacro

class QueryableMacro(val c: Context)
    extends TypeAttachment with Lifting with Unlifting with NormalizationMacro {

  import c.universe._
  
  def apply[T](implicit t: WeakTypeTag[T]) =
    attach[Queryable[T]](ast.Table(t.tpe.typeSymbol.name.toString): ast.Query)

  def filter[T](f: c.Expr[T => Any])(implicit t: WeakTypeTag[T]) = {
    f.tree match {
      case q"($input) => $body" if (input.name.toString.contains("ifrefutable")) =>
        c.prefix.tree
      case q"(${ alias: ast.Ident }) => ${ body: ast.Expr }" =>
        toQueryable[T](ast.Filter(detach[Query](c.prefix.tree), alias, body))
    }
  }

  def map[T, R](f: c.Expr[T => R])(implicit t: WeakTypeTag[T], r: WeakTypeTag[R]) = {
    f.tree match {
      case q"(${ alias: ast.Ident }) => ${ body: ast.Expr }" =>
        toQueryable[R](ast.Map(detach[Query](c.prefix.tree), alias, body))
    }
  }

  def flatMap[T, R](f: c.Expr[T => Queryable[R]])(implicit t: WeakTypeTag[T], r: WeakTypeTag[R]) = {
    f.tree match {
      case q"(${ alias: ast.Ident }) => ${ matchAlias: ast.Ident } match { case (..$a) => $body }" if (alias == matchAlias) =>
        val aliases =
          a.map {
            case Bind(name, _) =>
              ast.Ident(name.decodedName.toString)
          }
        val query = detach[Query](body)
        val reduction =
          for ((a, i) <- aliases.zipWithIndex) yield {
            a -> ast.Property(alias, s"_${i + 1}")
          }
        toQueryable[R](ast.FlatMap(detach[Query](c.prefix.tree), alias, BetaReduction(query)(reduction.toMap)))
      case q"(${ alias: ast.Ident }) => $body" =>
        toQueryable[R](ast.FlatMap(detach[Query](c.prefix.tree), alias, detach[Query](body)))
    }
  }

  private def toQueryable[T](query: Query)(implicit t: WeakTypeTag[T]) =
    attach[Queryable[T]](query)

}
