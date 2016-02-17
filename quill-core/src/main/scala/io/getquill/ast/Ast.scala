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

//************************************************************

sealed trait Query extends Ast

case class Entity(name: String, alias: Option[String] = None, properties: List[PropertyAlias] = List()) extends Query

case class PropertyAlias(property: String, alias: String)

case class Filter(query: Ast, alias: Ident, body: Ast) extends Query

case class Map(query: Ast, alias: Ident, body: Ast) extends Query

case class FlatMap(query: Ast, alias: Ident, body: Ast) extends Query

case class SortBy(query: Ast, alias: Ident, criterias: Ast, ordering: Ordering) extends Query

sealed trait Ordering
case class TupleOrdering(elems: List[Ordering]) extends Ordering

sealed trait PropertyOrdering extends Ordering
case object Asc extends PropertyOrdering
case object Desc extends PropertyOrdering
case object AscNullsFirst extends PropertyOrdering
case object DescNullsFirst extends PropertyOrdering
case object AscNullsLast extends PropertyOrdering
case object DescNullsLast extends PropertyOrdering

case class GroupBy(query: Ast, alias: Ident, body: Ast) extends Query

case class Aggregation(operator: AggregationOperator, ast: Ast) extends Query

case class Take(query: Ast, n: Ast) extends Query

case class Drop(query: Ast, n: Ast) extends Query

case class Union(a: Ast, b: Ast) extends Query

case class UnionAll(a: Ast, b: Ast) extends Query

case class Join(typ: JoinType, a: Ast, b: Ast, aliasA: Ident, aliasB: Ident, on: Ast) extends Query

//************************************************************

case class Infix(parts: List[String], params: List[Ast]) extends Ast

case class Function(params: List[Ident], body: Ast) extends Ast

case class Ident(name: String) extends Ast

case class Property(ast: Ast, name: String) extends Ast

case class OptionOperation(t: OptionOperationType, ast: Ast, alias: Ident, body: Ast) extends Ast

case class If(condition: Ast, `then`: Ast, `else`: Ast) extends Ast

//************************************************************

sealed trait Operation extends Ast

case class UnaryOperation(operator: UnaryOperator, ast: Ast) extends Operation
case class BinaryOperation(a: Ast, operator: BinaryOperator, b: Ast) extends Operation
case class FunctionApply(function: Ast, values: List[Ast]) extends Operation

//************************************************************

sealed trait Value extends Ast

case class Constant(v: Any) extends Value

object NullValue extends Value

case class Tuple(values: List[Ast]) extends Value

case class Set(values: List[Ast]) extends Value

//************************************************************

sealed trait Action extends Ast

case class Update(query: Ast) extends Action
case class Insert(query: Ast) extends Action
case class Delete(query: Ast) extends Action

case class AssignedAction(action: Ast, assignments: List[Assignment]) extends Action

case class Assignment(input: Ident, property: String, value: Ast)

//************************************************************

case class Dynamic(tree: Any) extends Ast
