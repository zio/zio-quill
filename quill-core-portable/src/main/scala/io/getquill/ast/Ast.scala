package io.getquill.ast

import io.getquill.NamingStrategy
import io.getquill.quat.Quat

//************************************************************

sealed trait Ast {

  def quat: Quat

  /**
   * Return a copy of this AST element with any opinions that it may have set to their neutral position.
   * Return the object itself if it has no opinions.
   */
  def neutral: Ast = this

  /**
   * Set all opinions of this element and every element in the subtree to the neutral position.
   */
  final def neutralize: Ast =
    new StatelessTransformer {
      override def apply(a: Ast) =
        super.apply(a.neutral)
    }.apply(this)

  override def toString = {
    import io.getquill.MirrorIdiom._
    import io.getquill.idiom.StatementInterpolator._
    implicit def externalTokenizer: Tokenizer[External] =
      Tokenizer[External](_ => stmt"?")
    implicit val namingStrategy: NamingStrategy = io.getquill.Literal
    this.token.toString
  }
}

//************************************************************

sealed trait Query extends Ast

sealed trait Terminal extends Ast {
  def withQuat(newQuat: Quat): Terminal
}

object Terminal {
  def unapply(ast: Ast): Option[Terminal] =
    ast match {
      case t: Terminal => Some(t)
      case _           => None
    }
}

object BottomTypedTerminal {
  def unapply(ast: Ast): Option[Terminal] =
    ast match {
      case t: Terminal if (t.quat == Quat.Null || t.quat == Quat.Generic) =>
        Some(t)
      case _ =>
        None
    }
}

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
case class Entity(name: String, properties: List[PropertyAlias], quat: Quat.Probity) extends Query { // TODO Quat - quat cannot be part of equals/hashcode
  // Technically this should be part of the Entity case class but due to the limitations of how
  // scala creates companion objects, the apply/unapply wouldn't be able to work correctly.
  def renameable: Renameable = Renameable.neutral

  override def neutral: Entity =
    new Entity(name, properties, quat)

  // TODO Quat add quat to equals function here? (note that if you do the tests will test for it)
  override def equals(that: Any) =
    that match {
      case e: Entity =>
        (e.name, e.properties, e.renameable) == ((name, properties, renameable))
      case _ => false
    }

  // TODO Quat add quat to hash code function here?
  override def hashCode = (name, properties, renameable).hashCode()

  // TODO Quat Test independently
  def syncToQuat: Entity = {
    val newQuat =
      properties.foldLeft(quat) {
        case (quat, PropertyAlias(path, alias)) =>
          quat match {
            case e: Quat.Error   => e
            case p: Quat.Product => p.stashRename(path, alias)
          }
      }
    Entity.Opinionated(name, properties, newQuat, renameable)
  }
}

object Entity {
  def apply(name: String, properties: List[PropertyAlias], quat: Quat.Probity) =
    new Entity(name, properties, quat)
  def unapply(e: Entity) = Some((e.name, e.properties, e.quat))

  object Opinionated {
    def apply(
      name:          String,
      properties:    List[PropertyAlias],
      quat:          Quat.Probity,
      renameableNew: Renameable
    ) =
      new Entity(name, properties, quat) {
        override def renameable: Renameable = renameableNew
      }
    def unapply(e: Entity) =
      Some((e.name, e.properties, e.quat, e.renameable))
  }
}

case class PropertyAlias(path: List[String], alias: String)

case class Filter(query: Ast, alias: Ident, body: Ast) extends Query { def quat = query.quat }

case class Map(query: Ast, alias: Ident, body: Ast) extends Query { def quat = body.quat }

case class FlatMap(query: Ast, alias: Ident, body: Ast) extends Query { def quat = body.quat }

case class ConcatMap(query: Ast, alias: Ident, body: Ast) extends Query { def quat = body.quat } // TODO Quat check out if quat is supposed to come from the query or the body?

case class SortBy(query: Ast, alias: Ident, criterias: Ast, ordering: Ast)
  extends Query { def quat = query.quat }

sealed trait Ordering extends Ast { def quat = Quat.Value }
case class TupleOrdering(elems: List[Ordering]) extends Ordering

sealed trait PropertyOrdering extends Ordering
case object Asc extends PropertyOrdering
case object Desc extends PropertyOrdering
case object AscNullsFirst extends PropertyOrdering
case object DescNullsFirst extends PropertyOrdering
case object AscNullsLast extends PropertyOrdering
case object DescNullsLast extends PropertyOrdering

case class GroupBy(query: Ast, alias: Ident, body: Ast) extends Query { def quat = Quat.Tuple(body.quat, query.quat) }

case class Aggregation(operator: AggregationOperator, ast: Ast) extends Query { def quat = Quat.Value }

case class Take(query: Ast, n: Ast) extends Query { def quat = query.quat }

case class Drop(query: Ast, n: Ast) extends Query { def quat = query.quat }

case class Union(a: Ast, b: Ast) extends Query { def quat = a.quat } // a and b quats should be same

case class UnionAll(a: Ast, b: Ast) extends Query { def quat = a.quat } // a and b quats should be same

case class Join(
  typ:    JoinType,
  a:      Ast,
  b:      Ast,
  aliasA: Ident,
  aliasB: Ident,
  on:     Ast
)
  extends Query { def quat = Quat.Tuple(a.quat, b.quat) }

case class FlatJoin(typ: JoinType, a: Ast, aliasA: Ident, on: Ast) extends Query { def quat = a.quat }

case class Distinct(a: Ast) extends Query { def quat = a.quat }

case class Nested(a: Ast) extends Query { def quat = a.quat }

//************************************************************

case class Infix(parts: List[String], params: List[Ast], pure: Boolean, quat: Quat) extends Ast

case class Function(params: List[Ident], body: Ast) extends Ast { def quat = body.quat }

case class Ident(name: String, quat: Quat) extends Terminal with Ast {
  def visibility: Visibility = Visibility.Visible

  override def neutral: Ident =
    new Ident(name, quat: Quat) {
      override def visibility: Visibility = Visibility.neutral
    }

  override def equals(that: Any) =
    that match {
      case p: Ident => (p.name, p.visibility) == ((name, visibility))
      case _        => false
    }

  override def hashCode = (name, visibility).hashCode()

  override def withQuat(newQuat: Quat) =
    Ident.Opinionated(this.name, newQuat, this.visibility) // TODO Quat quat cannot be part of equals/hashcode (maybe add it to secondary args list?)
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
  def apply(name: String, quat: Quat = Quat.Value) = new Ident(name, quat)
  def unapply(p: Ident) = Some((p.name, p.quat))

  object Opinionated {
    def apply(name: String, quat: Quat, visibilityNew: Visibility) =
      new Ident(name, quat) {
        override def visibility: Visibility = visibilityNew
      }
    def unapply(p: Ident) =
      Some((p.name, p.quat, p.visibility))
  }
}

// Like identity but is but defined in a clause external to the query. Currently this is used
// for 'returning' clauses to define properties being returned.
case class ExternalIdent(name: String, quat: Quat) extends Ast {
  def renameable: Renameable = Renameable.neutral

  override def equals(that: Any) =
    that match {
      case e: ExternalIdent => (e.name, e.renameable) == ((name, renameable))
      case _                => false
    }

  override def hashCode = (name, renameable).hashCode()
}

object ExternalIdent {
  def apply(name: String, quat: Quat) = new ExternalIdent(name, quat)
  def unapply(e: ExternalIdent) = Some((e.name, e.quat))

  object Opinionated {
    def apply(name: String, quat: Quat, rename: Renameable) =
      new ExternalIdent(name, quat) {
        override def renameable: Renameable = rename
      }

    def unapply(e: ExternalIdent) = Some((e.name, e.quat, e.renameable))
  }
}

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

  def quat = ast.quat.lookup(name)

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
      case p: Property =>
        (p.ast, p.name, p.renameable, p.visibility) == (
          (
            ast,
            name,
            renameable,
            visibility
          )
        )
      case _ => false
    }

  override def hashCode = (ast, name, renameable, visibility).hashCode()
}

object Property {
  def apply(ast: Ast, name: String) = new Property(ast, name)
  def unapply(p: Property) = Some((p.ast, p.name))

  object Opinionated {
    def apply(
      ast:           Ast,
      name:          String,
      renameableNew: Renameable,
      visibilityNew: Visibility
    ) =
      new Property(ast, name) {
        override def renameable: Renameable = renameableNew
        override def visibility: Visibility = visibilityNew
      }
    def unapply(p: Property) =
      Some((p.ast, p.name, p.renameable, p.visibility))
  }
}

sealed trait OptionOperation extends Ast
case class OptionFlatten(ast: Ast) extends OptionOperation { def quat = ast.quat }
case class OptionGetOrElse(ast: Ast, body: Ast) extends OptionOperation { def quat = body.quat }
case class OptionFlatMap(ast: Ast, alias: Ident, body: Ast)
  extends OptionOperation { def quat = body.quat }
case class OptionMap(ast: Ast, alias: Ident, body: Ast) extends OptionOperation { def quat = body.quat }
case class OptionForall(ast: Ast, alias: Ident, body: Ast)
  extends OptionOperation { def quat = body.quat }
case class OptionExists(ast: Ast, alias: Ident, body: Ast)
  extends OptionOperation { def quat = body.quat }
case class OptionContains(ast: Ast, body: Ast) extends OptionOperation { def quat = body.quat }
case class OptionIsEmpty(ast: Ast) extends OptionOperation { def quat = ast.quat }
case class OptionNonEmpty(ast: Ast) extends OptionOperation { def quat = ast.quat }
case class OptionIsDefined(ast: Ast) extends OptionOperation { def quat = ast.quat }
case class OptionTableFlatMap(ast: Ast, alias: Ident, body: Ast)
  extends OptionOperation { def quat = body.quat }
case class OptionTableMap(ast: Ast, alias: Ident, body: Ast)
  extends OptionOperation { def quat = body.quat }
case class OptionTableExists(ast: Ast, alias: Ident, body: Ast)
  extends OptionOperation { def quat = body.quat }
case class OptionTableForall(ast: Ast, alias: Ident, body: Ast)
  extends OptionOperation { def quat = body.quat }
case class OptionNone(quat: Quat) extends OptionOperation with Terminal { // TODO Quat - Quat cannot be part of equals/hashcode  (maybe add it to secondary args list? but here there's no first one)
  override def withQuat(newQuat: Quat) = this.copy(quat = newQuat)
}
case class OptionSome(ast: Ast) extends OptionOperation { def quat = ast.quat }
case class OptionApply(ast: Ast) extends OptionOperation { def quat = ast.quat }
case class OptionOrNull(ast: Ast) extends OptionOperation { def quat = ast.quat }
case class OptionGetOrNull(ast: Ast) extends OptionOperation { def quat = ast.quat }

sealed trait IterableOperation extends Ast
case class MapContains(ast: Ast, body: Ast) extends IterableOperation { def quat = body.quat }
case class SetContains(ast: Ast, body: Ast) extends IterableOperation { def quat = body.quat }
case class ListContains(ast: Ast, body: Ast) extends IterableOperation { def quat = body.quat }

case class If(condition: Ast, `then`: Ast, `else`: Ast) extends Ast { def quat = `then`.quat } // then and else clauses should have identical quats

case class Assignment(alias: Ident, property: Ast, value: Ast) extends Ast { def quat = Quat.Value } // TODO Quat. Not sure where this is used, should look into it

//************************************************************

sealed trait Operation extends Ast

case class UnaryOperation(operator: UnaryOperator, ast: Ast) extends Operation { def quat = Quat.Value } // TODO Quat can this be used in a non-scalar context
case class BinaryOperation(a: Ast, operator: BinaryOperator, b: Ast)
  extends Operation { def quat = Quat.Value } // TODO Quat can this be used in a non-scalar context
case class FunctionApply(function: Ast, values: List[Ast]) extends Operation { def quat = function.quat }

//************************************************************

sealed trait Value extends Ast

case class Constant(v: Any) extends Value { def quat = Quat.Value } // TODO Quat can this be a non scalar?

object NullValue extends Value { def quat = Quat.Null }

case class Tuple(values: List[Ast]) extends Value { def quat = Quat.Tuple(values.map(_.quat)) }

case class CaseClass(values: List[(String, Ast)]) extends Value { def quat = Quat.Product(values.map { case (k, v) => (k, v.quat) }) }

//************************************************************

case class Block(statements: List[Ast]) extends Ast { def quat = statements.last.quat } // TODO Quat, should it be the last one. Also, can the list be empty?

case class Val(name: Ident, body: Ast) extends Ast { def quat = body.quat } // TODO Quat, can this be non scalar?

//************************************************************

sealed trait Action extends Ast

case class Update(query: Ast, assignments: List[Assignment]) extends Action { def quat = query.quat } // TODO Quat quat in actions represents numeric values?
case class Insert(query: Ast, assignments: List[Assignment]) extends Action { def quat = query.quat }
case class Delete(query: Ast) extends Action { def quat = query.quat }

sealed trait ReturningAction extends Action {
  def action: Ast
  def alias: Ident
  def property: Ast
}
object ReturningAction {
  def unapply(returningClause: ReturningAction): Option[(Ast, Ident, Ast)] =
    returningClause match {
      case Returning(action, alias, property) => Some((action, alias, property))
      case ReturningGenerated(action, alias, property) =>
        Some((action, alias, property))
      case _ => None
    }

}
case class Returning(action: Ast, alias: Ident, property: Ast)
  extends ReturningAction { def quat = property.quat }
case class ReturningGenerated(action: Ast, alias: Ident, property: Ast)
  extends ReturningAction { def quat = property.quat }

case class Foreach(query: Ast, alias: Ident, body: Ast) extends Action { def quat = body.quat } // TODO Quat in what situations is this used?

case class OnConflict(
  insert: Ast,
  target: OnConflict.Target,
  action: OnConflict.Action
)
  extends Action { def quat = insert.quat } // TODO Quat is quat needed in this cases? In what circumstances would it be used?

object OnConflict {

  case class Excluded(alias: Ident) extends Ast {
    override def neutral: Ast =
      alias.neutral
    def quat = alias.quat
  }
  case class Existing(alias: Ident) extends Ast {
    override def neutral: Ast =
      alias.neutral
    def quat = alias.quat
  }

  sealed trait Target
  case object NoTarget extends Target
  case class Properties(props: List[Property]) extends Target

  sealed trait Action
  case object Ignore extends Action
  case class Update(assignments: List[Assignment]) extends Action
}
//************************************************************

case class Dynamic(tree: Any) extends Ast { def quat = Quat.Value } // TODO Quat how is this used?????

case class QuotedReference(tree: Any, ast: Ast) extends Ast { def quat = ast.quat } // TODO Quat how is this used?????

sealed trait External extends Ast

/***********************************************************************/
/*                      Only Quill 2                                   */
/***********************************************************************/

sealed trait Lift extends External with Terminal {
  val name: String
  val value: Any
}

sealed trait ScalarLift extends Lift with Terminal {
  val encoder: Any
}
case class ScalarValueLift(name: String, value: Any, encoder: Any, quat: Quat)
  extends ScalarLift {
  override def withQuat(newQuat: Quat) = this.copy(quat = newQuat) // TODO Quat quat cannot be part of equals/hashcode  (maybe add it to secondary args list?)
}
case class ScalarQueryLift(name: String, value: Any, encoder: Any, quat: Quat)
  extends ScalarLift {
  override def withQuat(newQuat: Quat) = this.copy(quat = newQuat) // TODO Quat quat cannot be part of equals/hashcode (maybe add it to secondary args list?)
}

sealed trait CaseClassLift extends Lift
case class CaseClassValueLift(name: String, value: Any, quat: Quat) extends CaseClassLift { // TODO Quat quat cannot be part of equals/hashcode (maybe add it to secondary args list?)
  override def withQuat(newQuat: Quat) = this.copy(quat = newQuat)
}
case class CaseClassQueryLift(name: String, value: Any, quat: Quat) extends CaseClassLift { // TODO Quat quat cannot be part of equals/hashcode (maybe add it to secondary args list?)
  override def withQuat(newQuat: Quat) = this.copy(quat = newQuat)
}

/***********************************************************************/
/*                      New for ProtoQuill                             */
/***********************************************************************/

sealed trait Tag extends External {
  val uid: String
}

case class ScalarTag(uid: String) extends Tag { def quat = Quat.Value } // TODO Quat actual quat should be passed into here
case class QuotationTag(uid: String) extends Tag { def quat = Quat.Value } // TODO Quat actual quat should be passed into here
