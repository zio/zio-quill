package io.getquill.ast

import io.getquill.ast.AstShow.astShow
import io.getquill.util.Show.Shower

//************************************************************

sealed trait Ast {
  override def toString = {
    import io.getquill.util.Show._
    import io.getquill.ast.AstShow._
    this.show
  }
}

case class Infix(parts: List[String], params: List[Ast]) extends Ast

//************************************************************

sealed trait Query extends Ast

case class Entity(name: String) extends Query

case class Filter(query: Ast, alias: Ident, body: Ast) extends Query

case class Map(query: Ast, alias: Ident, body: Ast) extends Query

case class FlatMap(query: Ast, alias: Ident, body: Ast) extends Query

case class SortBy(query: Ast, alias: Ident, criterias: Ast) extends Query

case class Reverse(query: Ast) extends Query

case class Take(query: Ast, n: Ast) extends Query

//************************************************************

case class Function(params: List[Ident], body: Ast) extends Ast
case class FunctionApply(function: Ast, values: List[Ast]) extends Ast

case class Ident(name: String) extends Ast
case class Property(ast: Ast, name: String) extends Ast

//************************************************************

sealed trait Operation extends Ast

case class UnaryOperation(operator: UnaryOperator, Ast: Ast) extends Operation
case class BinaryOperation(a: Ast, operator: BinaryOperator, b: Ast) extends Operation

//************************************************************

sealed trait Value extends Ast

case class Constant(v: Any) extends Value

object NullValue extends Value

case class Tuple(values: List[Ast]) extends Value

//************************************************************

sealed trait Action extends Ast

case class Update(query: Ast, assingments: List[Assignment]) extends Action
case class Insert(query: Ast, assingments: List[Assignment]) extends Action
case class Delete(query: Ast) extends Action

case class Assignment(property: String, value: Ast)
