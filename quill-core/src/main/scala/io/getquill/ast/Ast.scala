package io.getquill.ast

//************************************************************

sealed trait Ast

//************************************************************

sealed trait Query extends Ast

case class Table(name: String) extends Query

case class Filter(query: Ast, alias: Ident, body: Ast) extends Query

case class Map(query: Ast, alias: Ident, body: Ast) extends Query

case class FlatMap(query: Ast, alias: Ident, body: Ast) extends Query

//************************************************************

trait Function extends Ast

case class FunctionDef(params: List[Ident], body: Ast) extends Function
case class FunctionRef(ident: Ident) extends Function

//************************************************************

sealed trait Operation extends Ast

case class UnaryOperation(operator: UnaryOperator, Ast: Ast) extends Operation
case class BinaryOperation(a: Ast, operator: BinaryOperator, b: Ast) extends Operation
case class FunctionApply(function: Function, values: List[Ast]) extends Operation

//************************************************************

sealed trait Ref extends Ast

case class Property(Ast: Ast, name: String) extends Ref

case class Ident(name: String) extends Ref

//************************************************************

sealed trait Value extends Ref

case class Constant(v: Any) extends Value

object NullValue extends Value

case class Tuple(values: List[Ast]) extends Value

//************************************************************