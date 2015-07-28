package io.getquill.ast

sealed trait Parametrized {
  def params: List[Ident]
}

case class ParametrizedQuery(params: List[Ident], query: Query) extends Parametrized
case class ParametrizedExpr(params: List[Ident], expr: Expr) extends Parametrized