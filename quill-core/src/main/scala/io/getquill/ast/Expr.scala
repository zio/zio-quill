package io.getquill.ast

//************************************************************

sealed trait Expr

case class Subtract(a: Expr, b: Expr) extends Expr

case class Add(a: Expr, b: Expr) extends Expr

case class FunctionApply(ident: Ident, value: Expr) extends Expr

case class Equals(a: Expr, b: Expr) extends Expr

case class And(a: Expr, b: Expr) extends Expr

case class GreaterThan(a: Expr, b: Expr) extends Expr
case class GreaterThanOrEqual(a: Expr, b: Expr) extends Expr

case class LessThan(a: Expr, b: Expr) extends Expr
case class LessThanOrEqual(a: Expr, b: Expr) extends Expr

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