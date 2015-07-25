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

  def query[T](q: c.Expr[Queryable[T]])(implicit t: WeakTypeTag[T]) =
    q.tree match {
      case q"${ query: Query }" =>
        attach[Queryable[T]](query)
    }
}
