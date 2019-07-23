package io.getquill.ast

//************************************************************

sealed trait Ast {
  override def toString = {
    import io.getquill.MirrorIdiom._
    import io.getquill.idiom.StatementInterpolator._
    implicit def liftTokenizer: Tokenizer[Lift] =
      Tokenizer[Lift](_ => stmt"?")
    this.token.toString
  }
}

//************************************************************

sealed trait Query extends Ast

case class Entity(name: String, properties: List[PropertyAlias]) extends Query

case class PropertyAlias(path: List[String], alias: String)

case class Filter(query: Ast, alias: Ident, body: Ast) extends Query

case class Map(query: Ast, alias: Ident, body: Ast) extends Query

case class FlatMap(query: Ast, alias: Ident, body: Ast) extends Query

case class ConcatMap(query: Ast, alias: Ident, body: Ast) extends Query

case class SortBy(query: Ast, alias: Ident, criterias: Ast, ordering: Ast) extends Query

sealed trait Ordering extends Ast
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

case class FlatJoin(typ: JoinType, a: Ast, aliasA: Ident, on: Ast) extends Query

case class Distinct(a: Ast) extends Query

case class Nested(a: Ast) extends Query

//************************************************************

case class Infix(parts: List[String], params: List[Ast], pure:Boolean) extends Ast

case class Function(params: List[Ident], body: Ast) extends Ast

case class Ident(name: String) extends Ast

// Like identity but is but defined in a clause external to the query. Currently this is used
// for 'returning' clauses to define properties being retruned.
case class ExternalIdent(name: String) extends Ast

case class Property(ast: Ast, name: String) extends Ast

sealed trait OptionOperation extends Ast
case class OptionFlatten(ast: Ast) extends OptionOperation
case class OptionGetOrElse(ast: Ast, body: Ast) extends OptionOperation
case class OptionFlatMap(ast: Ast, alias: Ident, body: Ast) extends OptionOperation
case class OptionMap(ast: Ast, alias: Ident, body: Ast) extends OptionOperation
case class OptionForall(ast: Ast, alias: Ident, body: Ast) extends OptionOperation
case class OptionExists(ast: Ast, alias: Ident, body: Ast) extends OptionOperation
case class OptionContains(ast: Ast, body: Ast) extends OptionOperation
case class OptionIsEmpty(ast: Ast) extends OptionOperation
case class OptionNonEmpty(ast: Ast) extends OptionOperation
case class OptionIsDefined(ast: Ast) extends OptionOperation
case class OptionTableFlatMap(ast: Ast, alias: Ident, body: Ast) extends OptionOperation
case class OptionTableMap(ast: Ast, alias: Ident, body: Ast) extends OptionOperation
case class OptionTableExists(ast: Ast, alias: Ident, body: Ast) extends OptionOperation
case class OptionTableForall(ast: Ast, alias: Ident, body: Ast) extends OptionOperation
object OptionNone extends OptionOperation
case class OptionSome(ast: Ast) extends OptionOperation
case class OptionApply(ast: Ast) extends OptionOperation
case class OptionOrNull(ast: Ast) extends OptionOperation
case class OptionGetOrNull(ast: Ast) extends OptionOperation

sealed trait TraversableOperation extends Ast
case class MapContains(ast: Ast, body: Ast) extends TraversableOperation
case class SetContains(ast: Ast, body: Ast) extends TraversableOperation
case class ListContains(ast: Ast, body: Ast) extends TraversableOperation

case class If(condition: Ast, `then`: Ast, `else`: Ast) extends Ast

case class Assignment(alias: Ident, property: Ast, value: Ast) extends Ast

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
case class CaseClass(values: List[(String, Ast)]) extends Value

//************************************************************

case class Block(statements: List[Ast]) extends Ast

case class Val(name: Ident, body: Ast) extends Ast

//************************************************************

sealed trait Action extends Ast

case class Update(query: Ast, assignments: List[Assignment]) extends Action
case class Insert(query: Ast, assignments: List[Assignment]) extends Action
case class Delete(query: Ast) extends Action

sealed trait ReturningAction extends Action {
  def action: Ast
  def alias: Ident
  def property: Ast
}
object ReturningAction {
  def unapply(returningClause: ReturningAction): Option[(Ast, Ident, Ast)] =
    returningClause match {
      case Returning(action, alias, property) => Some((action, alias, property))
      case ReturningGenerated(action, alias, property) => Some((action, alias, property))
      case _ => None
    }

}
case class Returning(action: Ast, alias: Ident, property: Ast) extends ReturningAction
case class ReturningGenerated(action: Ast, alias: Ident, property: Ast) extends ReturningAction

case class Foreach(query: Ast, alias: Ident, body: Ast) extends Action

case class OnConflict(insert: Ast, target: OnConflict.Target, action: OnConflict.Action) extends Action
object OnConflict {

  case class Excluded(alias: Ident) extends Ast
  case class Existing(alias: Ident) extends Ast

  sealed trait Target
  case object NoTarget extends Target
  case class Properties(props: List[Property]) extends Target

  sealed trait Action
  case object Ignore extends Action
  case class Update(assignments: List[Assignment]) extends Action
}
//************************************************************

case class Dynamic(tree: Any) extends Ast

case class QuotedReference(tree: Any, ast: Ast) extends Ast

sealed trait Lift extends Ast {
  val name: String
  val value: Any
}

sealed trait ScalarLift extends Lift {
  val encoder: Any
}
case class ScalarValueLift(name: String, value: Any, encoder: Any) extends ScalarLift
case class ScalarQueryLift(name: String, value: Any, encoder: Any) extends ScalarLift

sealed trait CaseClassLift extends Lift
case class CaseClassValueLift(name: String, value: Any) extends CaseClassLift
case class CaseClassQueryLift(name: String, value: Any) extends CaseClassLift
