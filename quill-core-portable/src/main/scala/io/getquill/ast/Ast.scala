package io.getquill.ast

import io.getquill.NamingStrategy
import io.getquill.quat.Quat

//************************************************************

sealed trait Ast {

  def quat: Quat

  def countQuatFields: Int =
    CollectAst(this) {
      case c: Constant           => c.quat.countFields
      case a: Ident              => a.quat.countFields
      case o: OptionNone         => o.quat.countFields
      case l: ScalarValueLift    => l.quat.countFields
      case l: ScalarQueryLift    => l.quat.countFields
      case l: CaseClassValueLift => l.quat.countFields
      case l: CaseClassQueryLift => l.quat.countFields
    }.sum

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
  def withQuat(quat: => Quat): Terminal
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
      case t: Terminal if (t.quat == Quat.Null || t.quat == Quat.Generic || t.quat == Quat.Unknown) =>
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
class Entity(val name: String, val properties: List[PropertyAlias])(theQuat: => Quat.Product) extends Query {
  def quat: Quat.Product = theQuat
  private def id = Entity.Id(name, properties)
  // Technically this should be part of the Entity case class but due to the limitations of how
  // scala creates companion objects, the apply/unapply wouldn't be able to work correctly.
  def renameable: Renameable = Renameable.neutral

  def copy(name: String = this.name, properties: List[PropertyAlias] = this.properties, quat: Quat.Product = this.quat) =
    Entity.Opinionated(name, properties, quat, this.renameable)

  override def equals(that: Any) =
    that match {
      case e: Entity => this.id == e.id
      case _         => false
    }

  override def hashCode = id.hashCode()

  def syncToQuat: Entity = {
    import io.getquill.quat.QuatOps.Implicits._

    // Take each path (name.first -> theFirst, name.last -> theLast) to:
    //   (name -> (first -> theFirst)), (name -> (first -> theLast))
    val tailPaths =
      properties.map {
        case PropertyAlias(path, alias) =>
          (path.dropRight(1), path.last -> alias)
      }

    // Group each path that we have taken:
    // (name -> (first -> theFirst)), (name -> (first -> theLast))
    //   => (name -> (first -> theFirst, last -> theLast))
    val groupedTailPaths = tailPaths.groupBy(_._1).map(kv => (kv._1, kv._2.map(r => r._2))).toList

    lazy val quat: Quat.Product =
      groupedTailPaths.foldLeft(this.quat) {
        case (quat, (renamePath, renames)) =>
          quat.renameAtPath(renamePath, renames)
      }

    Entity.Opinionated(name, properties, quat, renameable)
  }
}

object Entity {
  private case class Id(name: String, properties: List[PropertyAlias])

  def apply(name: String, properties: List[PropertyAlias], quat: => Quat.Product): Entity =
    new Entity(name, properties)(quat)

  def unapply(e: Entity) = Some((e.name, e.properties, e.quat))

  object Opinionated {
    def apply(
      name:          String,
      properties:    List[PropertyAlias],
      quat:          => Quat.Product,
      renameableNew: Renameable
    ): Entity =
      new Entity(name, properties)(quat) {
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

case class ConcatMap(query: Ast, alias: Ident, body: Ast) extends Query { def quat = body.quat }

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

class Infix(val parts: List[String], val params: List[Ast], val pure: Boolean)(theQuat: => Quat) extends Ast {
  def quat: Quat = theQuat
  private def id = Infix.Id(parts, params, pure)

  override def equals(that: Any) =
    that match {
      case p: Infix => this.id == p.id
      case _        => false
    }

  override def hashCode = id.hashCode()
  def copy(parts: List[String] = this.parts, params: List[Ast] = this.params, pure: Boolean = this.pure, quat: Quat = this.quat) =
    Infix(parts, params, pure, quat)
}
object Infix {
  private case class Id(val parts: List[String], val params: List[Ast], val pure: Boolean)

  def apply(parts: List[String], params: List[Ast], pure: Boolean, quat: => Quat) =
    new Infix(parts, params, pure)(quat)

  def unapply(i: Infix) = Some((i.parts, i.params, i.pure, i.quat))
}

case class Function(params: List[Ident], body: Ast) extends Ast { def quat = body.quat }

class Ident(val name: String)(theQuat: => Quat) extends Terminal with Ast {
  def quat: Quat = theQuat
  private val id = Ident.Id(name)
  def visibility: Visibility = Visibility.Visible

  override def equals(that: Any) =
    that match {
      case p: Ident => this.id == p.id
      case _        => false
    }

  override def hashCode = id.hashCode()

  override def withQuat(quat: => Quat): Ident = {
    Ident.Opinionated(this.name, quat, this.visibility)
  }

  // need to define a copy which will propogate current value of visibility into the copy
  def copy(name: String = this.name, quat: => Quat = this.quat): Ident =
    Ident.Opinionated(name, quat, this.visibility)
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
  private case class Id(name: String)
  def apply(name: String, quat: => Quat = Quat.Value) = new Ident(name)(quat)
  def unapply(p: Ident) = Some((p.name, p.quat))

  object Opinionated {
    def apply(name: String, quatNew: => Quat, visibilityNew: Visibility) =
      new Ident(name)(quatNew) {
        override def visibility: Visibility = visibilityNew
      }
    def unapply(p: Ident) =
      Some((p.name, p.quat, p.visibility))
  }
}

// Like identity but is but defined in a clause external to the query. Currently this is used
// for 'returning' clauses to define properties being returned.
class ExternalIdent(val name: String)(theQuat: => Quat) extends Ast {
  def quat: Quat = theQuat
  private def id = ExternalIdent.Id(name)
  def renameable: Renameable = Renameable.neutral

  override def equals(that: Any) =
    that match {
      case e: ExternalIdent => this.id == e.id
      case _                => false
    }

  override def hashCode = id.hashCode()

  // need to define a copy which will propogate current value of visibility into the copy
  def copy(name: String = this.name, quat: => Quat = this.quat): ExternalIdent =
    ExternalIdent.Opinionated(name, quat, this.renameable)
}

object ExternalIdent {
  private case class Id(name: String)

  def apply(name: String, quat: => Quat) = new ExternalIdent(name)(quat)
  def unapply(e: ExternalIdent) = Some((e.name, e.quat))

  object Opinionated {
    def apply(name: String, quat: => Quat, rename: Renameable) =
      new ExternalIdent(name)(quat) {
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
 * That said, Opinions should be transparent in most steps of the normalization.
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
  def prevName = ast.quat.beforeRenamed(name)

  // Properties that are 'Hidden' are used for embedded objects whose path should not be expressed
  // during SQL Tokenization.
  def visibility: Visibility = Visibility.Visible

  def copy(ast: Ast = this.ast, name: String = this.name): Property =
    Property.Opinionated(ast, name, this.renameable, this.visibility)
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
case object OptionNoneId
class OptionNone(theQuat: => Quat) extends OptionOperation with Terminal {
  def quat: Quat = theQuat
  override def withQuat(quat: => Quat) = this.copy(quat = quat)
  override def equals(obj: Any): Boolean =
    obj match {
      case e: OptionNone => true
      case _             => false
    }
  override def hashCode(): Int = OptionNoneId.hashCode()
  def copy(quat: => Quat = this.quat) = OptionNone(quat)
}
object OptionNone {
  def apply(quat: => Quat): OptionNone = new OptionNone(quat)
  def unapply(on: OptionNone) = Some(on.quat)
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

case class Assignment(alias: Ident, property: Ast, value: Ast) extends Ast { def quat = Quat.Value }

//************************************************************

sealed trait Operation extends Ast

case class UnaryOperation(operator: UnaryOperator, ast: Ast) extends Operation { def quat = Quat.BooleanExpression }

case class BinaryOperation(a: Ast, operator: BinaryOperator, b: Ast) extends Operation {
  import BooleanOperator._
  import NumericOperator._
  import StringOperator.`startsWith`
  import SetOperator.`contains`

  def quat = operator match {
    case EqualityOperator.`==` | EqualityOperator.`!=`
      | `&&` | `||`
      | `>` | `>=` | `<` | `<=`
      | `startsWith`
      | `contains` =>
      Quat.BooleanExpression
    case _ =>
      Quat.Value
  }
}
case class FunctionApply(function: Ast, values: List[Ast]) extends Operation { def quat = function.quat }

//************************************************************

sealed trait Value extends Ast

class Constant(val v: Any)(theQuat: => Quat) extends Value {
  def quat: Quat = theQuat
  private val id = Constant.Id(v)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: Constant => e.id == this.id
      case _           => false
    }
}

object Constant {
  private case class Id(v: Any)

  def apply(v: Any, quat: => Quat): Constant = new Constant(v)(quat)
  def unapply(const: Constant) = Some((const.v, const.quat))

  def auto(v: Any): Constant = {
    lazy val theQuat = if (v.isInstanceOf[Boolean]) Quat.BooleanValue else Quat.Value
    new Constant(v)(theQuat)
  }
}

object NullValue extends Value { def quat = Quat.Null }

case class Tuple(values: List[Ast]) extends Value { def quat = Quat.Tuple(values.map(_.quat)) }

case class CaseClass(values: List[(String, Ast)]) extends Value { def quat = Quat.Product(values.map { case (k, v) => (k, v.quat) }) }

//************************************************************

case class Block(statements: List[Ast]) extends Ast { def quat = statements.last.quat } // Note. Assuming Block is not Empty

case class Val(name: Ident, body: Ast) extends Ast { def quat = body.quat }

//************************************************************

sealed trait Action extends Ast

// Note, technically return type of Actions for most Actions is a Int value but Quat here is used for Retruning Quat types
case class Update(query: Ast, assignments: List[Assignment]) extends Action { def quat = query.quat }
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

case class Foreach(query: Ast, alias: Ident, body: Ast) extends Action { def quat = body.quat }

case class OnConflict(
  insert: Ast,
  target: OnConflict.Target,
  action: OnConflict.Action
)
  extends Action { def quat = insert.quat }

object OnConflict {

  case class Excluded(alias: Ident) extends Ast {
    def quat = alias.quat
    override def equals(obj: Any): Boolean =
      obj match {
        case e: Excluded => e.alias == alias
        case e: Ident    => e == alias
        case _           => false
      }
    override def hashCode(): Int = alias.hashCode
  }
  case class Existing(alias: Ident) extends Ast {
    def quat = alias.quat
    override def equals(obj: Any): Boolean =
      obj match {
        case e: Existing => e.alias == alias
        case e: Ident    => e == alias
        case _           => false
      }
    override def hashCode(): Int = alias.hashCode
  }

  sealed trait Target
  case object NoTarget extends Target
  case class Properties(props: List[Property]) extends Target

  sealed trait Action
  case object Ignore extends Action
  case class Update(assignments: List[Assignment]) extends Action
}
//************************************************************

class Dynamic(val tree: Any)(theQuat: => Quat) extends Ast {
  def quat = theQuat
}

object Dynamic {
  def apply(tree: Any, quat: => Quat): Dynamic = new Dynamic(tree)(quat)
  def unapply(d: Dynamic) = Some((d.tree, d.quat))

  def apply(v: Any): Dynamic = {
    lazy val theQuat = if (v.isInstanceOf[Boolean]) Quat.BooleanValue else Quat.Value
    new Dynamic(v)(theQuat)
  }
}

case class QuotedReference(tree: Any, ast: Ast) extends Ast { def quat = ast.quat }

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

class ScalarValueLift(val name: String, val value: Any, val encoder: Any)(theQuat: => Quat)
  extends ScalarLift {
  def quat: Quat = theQuat
  override def withQuat(quat: => Quat) = this.copy(quat = quat)

  private val id = ScalarValueLift.Id(name, value, encoder)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: ScalarValueLift => e.id == this.id
      case _                  => false
    }
  def copy(name: String = this.name, value: Any = this.value, encoder: Any = this.encoder, quat: => Quat = this.quat) =
    ScalarValueLift(name, value, encoder, quat)
}
object ScalarValueLift {
  private case class Id(name: String, value: Any, encoder: Any)
  def apply(name: String, value: Any, encoder: Any, quat: => Quat): ScalarValueLift = new ScalarValueLift(name, value, encoder)(quat)
  def unapply(svl: ScalarValueLift) = Some((svl.name, svl.value, svl.encoder, svl.quat))
}

class ScalarQueryLift(val name: String, val value: Any, val encoder: Any)(theQuat: => Quat)
  extends ScalarLift {
  def quat: Quat = theQuat
  override def withQuat(quat: => Quat) = this.copy(quat = quat)

  private val id = ScalarQueryLift.Id(name, value, encoder)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: ScalarQueryLift => e.id == this.id
      case _                  => false
    }

  def copy(name: String = this.name, value: Any = this.value, encoder: Any = this.encoder, quat: => Quat = this.quat) =
    ScalarQueryLift(name, value, encoder, quat)
}
object ScalarQueryLift {
  private case class Id(name: String, value: Any, encoder: Any)
  def apply(name: String, value: Any, encoder: Any, quat: => Quat): ScalarQueryLift = new ScalarQueryLift(name, value, encoder)(quat)
  def unapply(l: ScalarQueryLift) = Some((l.name, l.value, l.encoder, l.quat))
}

sealed trait CaseClassLift extends Lift

class CaseClassValueLift(val name: String, val value: Any)(theQuat: => Quat) extends CaseClassLift {
  def quat: Quat = theQuat
  override def withQuat(quat: => Quat) = this.copy(quat = quat)

  private val id = CaseClassValueLift.Id(name, value)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: CaseClassValueLift => e.id == this.id
      case _                     => false
    }
  def copy(name: String = this.name, value: Any = this.value, quat: => Quat = this.quat) =
    CaseClassValueLift(name, value, quat)
}
object CaseClassValueLift {
  private case class Id(name: String, value: Any)
  def apply(name: String, value: Any, quat: => Quat): CaseClassValueLift = new CaseClassValueLift(name, value)(quat)
  def unapply(l: CaseClassValueLift) = Some((l.name, l.value, l.quat))
}

class CaseClassQueryLift(val name: String, val value: Any)(theQuat: => Quat) extends CaseClassLift {
  def quat: Quat = theQuat
  override def withQuat(quat: => Quat) = this.copy(quat = quat)

  private val id = CaseClassQueryLift.Id(name, value)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: CaseClassQueryLift => e.id == this.id
      case _                     => false
    }

  def copy(name: String = this.name, value: Any = this.value, quat: => Quat = this.quat) =
    CaseClassQueryLift(name, value, quat)
}

object CaseClassQueryLift {
  private case class Id(name: String, value: Any)
  def apply(name: String, value: Any, quat: => Quat): CaseClassQueryLift = new CaseClassQueryLift(name, value)(quat)
  def unapply(l: CaseClassQueryLift) = Some((l.name, l.value, l.quat))
}

/***********************************************************************/
/*                      New for ProtoQuill                             */
/***********************************************************************/

sealed trait Tag extends External {
  val uid: String
}

case class ScalarTagId(uid: String)
case class ScalarTag(uid: String) extends Tag {
  def quat = Quat.Value

  private val id = ScalarTagId(uid)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: ScalarTag => this.id == e.id
      case _            => false
    }
}

case class QuotationTagId(uid: String)
case class QuotationTag(uid: String) extends Tag {
  def quat = Quat.Value

  private val id = QuotationTagId(uid)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: QuotationTag => this.id == e.id
      case _               => false
    }
}
