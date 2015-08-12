package io.getquill.ast

//************************************************************

sealed trait Expr

case class QueryExpr(query: Query) extends Expr

//************************************************************

sealed trait Operation extends Expr

case class UnaryOperation(operator: UnaryOperator, expr: Expr) extends Operation
case class BinaryOperation(a: Expr, operator: BinaryOperator, b: Expr) extends Operation

//************************************************************

sealed trait Ref extends Expr

case class Property(expr: Expr, name: String) extends Ref

case class Ident(name: String) extends Ref

//************************************************************

sealed trait Value extends Ref

case class Constant(v: Any) extends Value

object NullValue extends Value

case class Tuple(values: List[Expr]) extends Value

//************************************************************