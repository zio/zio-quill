package io.getquill

import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Ident
import io.getquill.ast.Parametrized
import io.getquill.ast.ParametrizedExpr
import io.getquill.ast.ParametrizedQuery
import io.getquill.attach.TypeAttachment
import io.getquill.lifting.Lifting
import io.getquill.lifting.Unlifting

class PartialMacroCreate(val c: Context) extends TypeAttachment with Lifting with Unlifting {
  import c.universe._

  def create1[P1: WeakTypeTag, T: WeakTypeTag](f: c.Expr[P1 => T]) =
    create[P1, T](f)

  def create2[P1: WeakTypeTag, P2: WeakTypeTag, T: WeakTypeTag](f: c.Expr[(P1, P2) => T]) =
    create[(P1, P2), T](f)

  private def create[P, T](f: c.Expr[Any])(implicit p: WeakTypeTag[P], t: WeakTypeTag[T]): Tree =
    f.tree match {
      case q"(..${ inputs: List[ast.Ident] }) => $body" =>
        attach[Partial](create(inputs, body))
    }

  private def create(inputs: List[ast.Ident], body: Tree): Parametrized =
    body match {
      case q"${ expr: ast.Expr }"   => ParametrizedExpr(inputs, expr)
      case q"${ query: ast.Query }" => ParametrizedQuery(inputs, query)
    }
}
