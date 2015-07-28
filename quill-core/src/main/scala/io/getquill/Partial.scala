package io.getquill

import language.experimental.macros
import io.getquill.attach.Attachable
import io.getquill.ast.Query
import io.getquill.ast.Expr
import io.getquill.ast.ParametrizedQuery
import io.getquill.ast.ParametrizedExpr
import io.getquill.ast.Parametrized

trait Partial extends Attachable[Parametrized] {

  override def toString = {
    import util.Show._
    import ast.QueryShow._
    import ast.ExprShow._
    attachment match {
      case ParametrizedQuery(_, q) => q.show
      case ParametrizedExpr(_, e) => e.show
    } 
  }
}

trait Partial1[P1, T] extends Partial {

  def apply(pr1: P1): Any = macro PartialMacro.apply1[P1, T]
}

trait Partial2[P1, P2, T] extends Partial {

  def apply(pr1: P1, pr2: P2): Any = macro PartialMacro.apply2[P1, P2, T]
}
