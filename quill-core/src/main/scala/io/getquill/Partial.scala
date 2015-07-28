package io.getquill

import language.dynamics
import language.experimental.macros
import io.getquill.ast.Parametrized
import io.getquill.ast.ParametrizedExpr
import io.getquill.ast.ParametrizedQuery
import io.getquill.attach.Attachable

trait Partial[+T] extends Attachable[Parametrized] with Dynamic {

  def applyDynamic(selection: String)(args: Any*): Any = macro PartialMacro.apply[T]
  def applyDynamicNamed(selection: String)(args: (String, Any)*): Any = macro PartialMacro.applyNamed[T]

  override def toString = {
    import util.Show._
    import ast.QueryShow._
    import ast.ExprShow._
    attachment match {
      case ParametrizedQuery(_, q) => q.show
      case ParametrizedExpr(_, e)  => e.show
    }
  }
}

object Partial {

  def apply[P1, T](f: P1 => T): Any = macro PartialMacro.create1[P1, T]
  def apply[P1, P2, T](f: (P1, P2) => T): Any = macro PartialMacro.create2[P1, P2, T]
}
