package io.getquill.ast

import io.getquill.NamingStrategy

//************************************************************

sealed trait Ast {

  /**
   * Return a copy of this AST element with any opinions that it may have set to their neutral position.
   * Return the object itself if it has no opinions.
   */
  def neutral: Ast = this

  /**
   * Set all opinions of this element and every element in the subtree to the neutral position.
   */
  final def neutralize: Ast = new StatelessTransformer {
    override def apply(a: Ast) =
      super.apply(a.neutral)
  }.apply(this)

  override def toString = {
    import io.getquill.MirrorIdiom._
    import io.getquill.idiom.StatementInterpolator._
    implicit def liftTokenizer: Tokenizer[Lift] =
      Tokenizer[Lift](_ => stmt"?")
    implicit val namingStrategy: NamingStrategy = io.getquill.Literal
    this.token.toString
  }
}

//************************************************************

sealed trait Query extends Ast

/**
 * Entities represent the actual tables/views being selected.
 * Typically, something like:
 * <pre>`SELECT p.name FROM People p`</pre> comes from
 * something like:
 * <pre>`Map(Entity("People", Nil), Ident("p"), Property(Ident(p), "name"))`.</pre>
 * When you define a `querySchema`, the fields you mention inside become `PropertyAlias`s.
 * For example something like:
 * <pre>`querySchema[Person]("t_person", _.name -> "s_name")`</pre>
 * Becomes something like:
 * <pre>`Entity("t_person", List(PropertyAlias(List("name"), "s_name"))) { def renameable = Fixed }`</pre>
 * Note that Entity has an Opinion called `renameable` which will be the value `Fixed` when a `querySchema` is specified.
 * That means that even if the `NamingSchema` is `UpperCase`, the resulting query will select `t_person` as opposed
 * to `T_PERSON` or `Person`.
 */
case class Entity(name: String, properties: List[PropertyAlias]) extends Query {
  // Technically this should be part of the Entity case class but due to the limitations of how
  // scala creates companion objects, the apply/unapply wouldn't be able to work correctly.
  def renameable: Renameable = Renameable.neutral

  override def neutral: Entity =
    new Entity(name, properties)

  override def equals(that: Any) =
    that match {
      case e: Entity => (e.name, e.properties, e.renameable) == ((name, properties, renameable))
      case _         => false
    }

  override def hashCode = (name, properties, renameable).hashCode()
}

object Entity {
  def apply(name: String, properties: List[PropertyAlias]) = new Entity(name, properties)
  def unapply(e: Entity) = Some((e.name, e.properties))

  object Opinionated {
    def apply(name: String, properties: List[PropertyAlias], renameableNew: Renameable) =
      new Entity(name, properties) {
        override def renameable: Renameable = renameableNew
      }
    def unapply(e: Entity) =
      Some((e.name, e.properties, e.renameable))
  }
}

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

case class Infix(parts: List[String], params: List[Ast], pure: Boolean) extends Ast

case class Function(params: List[Ident], body: Ast) extends Ast

case class Ident(name: String) extends Ast {
  def visibility: Visibility = Visibility.Visible

  override def neutral: Ident =
    new Ident(name) {
      override def visibility: Visibility = Visibility.neutral
    }

  override def equals(that: Any) =
    that match {
      case p: Ident => (p.name, p.visibility) == ((name, visibility))
      case _        => false
    }

  override def hashCode = (name, visibility).hashCode()
}

/**
 * Ident represents a single variable name, this typically refers to a table but not always.
 * Invisible identities are a rare case where a user returns an embedded table from a map clause:
 *
 * <pre><code>
 *     case class Emb(id: Int, name: String) extends Embedded
 *     case class Parent(id: Int, name: String, emb: Emb) extends Embedded
 *     case class GrandParent(id: Int, par: Parent)
 *
 *     query[GrandParent]
 *         .map(g => g.par).distinct
 *         .map(p => (p.name, p.emb)).distinct
 *         .map(tup => (tup._1, tup._2)).distinct
 *     }
 * </code></pre>
 *
 * In these situations, the identity whose properties need to be expanded in the ExpandNestedQueries phase,
 * needs to be marked invisible.
 */
object Ident {
  def apply(name: String) = new Ident(name)
  def unapply(p: Ident) = Some((p.name))

  object Opinionated {
    def apply(name: String, visibilityNew: Visibility) =
      new Ident(name) {
        override def visibility: Visibility = visibilityNew
      }
    def unapply(p: Ident) =
      Some((p.name, p.visibility))
  }
}

// Like identity but is but defined in a clause external to the query. Currently this is used
// for 'returning' clauses to define properties being returned.
case class ExternalIdent(name: String) extends Ast

/**
 * An Opinion represents a piece of data that needs to be propagated through AST transformations but is not directly
 * related to how ASTs are transformed in most stages. For instance, `Renameable` controls how columns are named (i.e. whether to use a
 * `NamingStrategy` or not) after most of the SQL transformations are done. Some transformations (e.g. `RenameProperties`
 * will use `Opinions` or even modify them so that the correct kind of query comes out at the end of the normalizations.
 * That said, Opinions should be transparent in most steps of the normalization. In some cases e.g. `BetaReduction`,
 * AST elements need to be neutralized (i.e. set back to defaults in the entire tree) so that this works correctly.
 */
sealed trait Opinion[T]
sealed trait OpinionValues[T <: Opinion[T]] {
  def neutral: T
}

sealed trait Visibility extends Opinion[Visibility]
object Visibility extends OpinionValues[Visibility] {
  case object Visible extends Visibility with Opinion[Visibility]
  case object Hidden extends Visibility with Opinion[Visibility]

  override def neutral: Visibility = Visible
}

sealed trait Renameable extends Opinion[Renameable] {
  def fixedOr[T](plain: T)(otherwise: T) =
    this match {
      case Renameable.Fixed => plain
      case _                => otherwise
    }
}
object Renameable extends OpinionValues[Renameable] {
  case object Fixed extends Renameable with Opinion[Renameable]
  case object ByStrategy extends Renameable with Opinion[Renameable]

  override def neutral: Renameable = ByStrategy
}

/**
 * Properties generally represent column selection from a table or invocation of some kind of method from
 * some other object. Typically, something like
 * <pre>`SELECT p.name FROM People p`</pre> comes from
 * something like
 * <pre>`Map(Entity("People"), Ident("p"), Property(Ident(p), "name"))`</pre>
 * Properties also have
 * an Opinion about how the `NamingStrategy` affects their name. For example something like
 * `Property.Opinionated(Ident(p), "s_name", Fixed)` will become `p.s_name` even if the `NamingStrategy` is `UpperCase`
 * (whereas `Property(Ident(p), "s_name")` would become `p.S_NAME`). When Property is constructed without `Opinionated`
 * being used, the default opinion `ByStrategy` is used.
 */
case class Property(ast: Ast, name: String) extends Ast {
  // Technically this should be part of the Property case class but due to the limitations of how
  // scala creates companion objects, the apply/unapply wouldn't be able to work correctly.
  def renameable: Renameable = Renameable.neutral

  // Properties that are 'Hidden' are used for embedded objects whose path should not be expressed
  // during SQL Tokenization.
  def visibility: Visibility = Visibility.Visible

  override def neutral: Property =
    new Property(ast, name) {
      override def renameable = Renameable.neutral
      override def visibility: Visibility = Visibility.neutral
    }

  override def equals(that: Any) =
    that match {
      case p: Property => (p.ast, p.name, p.renameable, p.visibility) == ((ast, name, renameable, visibility))
      case _           => false
    }

  override def hashCode = (ast, name, renameable, visibility).hashCode()
}

object Property {
  def apply(ast: Ast, name: String) = new Property(ast, name)
  def unapply(p: Property) = Some((p.ast, p.name))

  object Opinionated {
    def apply(ast: Ast, name: String, renameableNew: Renameable, visibilityNew: Visibility) =
      new Property(ast, name) {
        override def renameable: Renameable = renameableNew
        override def visibility: Visibility = visibilityNew
      }
    def unapply(p: Property) =
      Some((p.ast, p.name, p.renameable, p.visibility))
  }
}

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

sealed trait IterableOperation extends Ast
case class MapContains(ast: Ast, body: Ast) extends IterableOperation
case class SetContains(ast: Ast, body: Ast) extends IterableOperation
case class ListContains(ast: Ast, body: Ast) extends IterableOperation

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

  case class Excluded(alias: Ident) extends Ast {
    override def neutral: Ast =
      alias.neutral
  }
  case class Existing(alias: Ident) extends Ast {
    override def neutral: Ast =
      alias.neutral
  }

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
