package io.getquill.ast

import io.getquill.NamingStrategy
import io.getquill.quat.Quat
import io.getquill.util.Messages

//************************************************************

sealed trait Ast {

  def quat: Quat
  def bestQuat: Quat

  final def countQuatFields: Int =
    CollectAst(this) {
      case c: Constant           => c.quat.countFields
      case a: Ident              => a.quat.countFields
      case o: OptionNone         => o.quat.countFields
      case l: ScalarValueLift    => l.quat.countFields
      case l: ScalarQueryLift    => l.quat.countFields
      case l: CaseClassValueLift => l.quat.countFields
      case l: CaseClassQueryLift => l.quat.countFields
    }.sum

  override final lazy val toString: String = {
    import io.getquill.MirrorIdiom._
    import io.getquill.idiom.StatementInterpolator._
    implicit def externalTokenizer: Tokenizer[External] = _externalTokenizer
    implicit val namingStrategy: NamingStrategy         = io.getquill.Literal
    this.token.toString
  }
}

object Ast {
  object LeafQuat {
    def unapply(ast: Ast): Option[Ast] =
      if (!ast.quat.isInstanceOf[Quat.Product])
        Some(ast)
      else
        None
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
 * Entities represent the actual tables/views being selected. Typically,
 * something like: <pre>`SELECT p.name FROM People p`</pre> comes from something
 * like: <pre>`Map(Entity("People", Nil), Ident("p"), Property(Ident(p),
 * "name"))`.</pre> When you define a `querySchema`, the fields you mention
 * inside become `PropertyAlias` s. For example something like:
 * <pre>`querySchema[Person]("t_person", _.name -> "s_name")`</pre> Becomes
 * something like: <pre>`Entity("t_person", List(PropertyAlias(List("name"),
 * "s_name"))) { def renameable = Fixed }`</pre> Note that Entity has an Opinion
 * called `renameable` which will be the value `Fixed` when a `querySchema` is
 * specified. That means that even if the `NamingSchema` is `UpperCase`, the
 * resulting query will select `t_person` as opposed to `T_PERSON` or `Person`.
 */

final class Entity(val name: String, val properties: List[PropertyAlias])(theQuat: => Quat.Product)(
  val renameable: Renameable
) extends Query {
  override lazy val quat: Quat.Product = theQuat
  override def bestQuat: Quat          = quat

  private val id: Entity.Id = Entity.Id(name, properties)

  def copy(
    name: String = this.name,
    properties: List[PropertyAlias] = this.properties,
    quat: Quat.Product = this.quat
  ): Entity =
    Entity.Opinionated(name, properties, quat, this.renameable)

  override def equals(that: Any): Boolean =
    that match {
      case e: Entity => this.id == e.id
      case _         => false
    }

  override def hashCode: Int = id.hashCode()

  lazy val syncToQuat: Entity = {
    import io.getquill.quat.QuatOps.Implicits._

    // Take each path (name.first -> theFirst, name.last -> theLast) to:
    //   (name -> (first -> theFirst)), (name -> (first -> theLast))
    val tailPaths =
      properties.map { case PropertyAlias(path, alias) =>
        (path.dropRight(1), path.last -> alias)
      }

    // Group each path that we have taken:
    // (name -> (first -> theFirst)), (name -> (first -> theLast))
    //   => (name -> (first -> theFirst, last -> theLast))
    val groupedTailPaths = tailPaths.groupBy(_._1).map(kv => (kv._1, kv._2.map(r => r._2))).toList

    val newQuat =
      groupedTailPaths.foldLeft(this.quat) { case (quat, (renamePath, renames)) =>
        quat.renameAtPath(renamePath, renames)
      }
    Entity.Opinionated(name, properties, newQuat, renameable)
  }
}

object Entity {
  private case class Id(name: String, properties: List[PropertyAlias])

  def apply(name: String, properties: List[PropertyAlias], quat: => Quat.Product): Entity =
    new Entity(name, properties)(quat)(Renameable.neutral)

  def unapply(e: Entity): Option[(String, List[PropertyAlias], Quat.Product)] = Some((e.name, e.properties, e.quat))

  object Opinionated {
    def apply(
      name: String,
      properties: List[PropertyAlias],
      quat: => Quat.Product,
      renameableNew: Renameable
    ): Entity =
      new Entity(name, properties)(quat)(renameableNew)

    def unapply(e: Entity): Option[(String, List[PropertyAlias], Quat.Product, Renameable)] =
      Some((e.name, e.properties, e.quat, e.renameable))
  }
}

final case class PropertyAlias(path: List[String], alias: String)

final case class Filter(query: Ast, alias: Ident, body: Ast) extends Query {
  override def quat: Quat     = query.quat
  override def bestQuat: Quat = query.bestQuat
}

final case class Map(query: Ast, alias: Ident, body: Ast) extends Query {
  override def quat: Quat     = body.quat
  override def bestQuat: Quat = body.bestQuat
}

final case class FlatMap(query: Ast, alias: Ident, body: Ast) extends Query {
  override def quat: Quat     = body.quat
  override def bestQuat: Quat = body.bestQuat
}

final case class ConcatMap(query: Ast, alias: Ident, body: Ast) extends Query {
  override def quat: Quat     = body.quat
  override def bestQuat: Quat = body.bestQuat
}

final case class SortBy(query: Ast, alias: Ident, criteria: Ast, ordering: Ast) extends Query {
  override def quat: Quat     = query.quat
  override def bestQuat: Quat = query.bestQuat
}

sealed trait Ordering extends Ast {
  override def quat: Quat     = Quat.Value
  override def bestQuat: Quat = quat
}
final case class TupleOrdering(elems: List[Ordering]) extends Ordering

sealed trait PropertyOrdering extends Ordering
case object Asc               extends PropertyOrdering
case object Desc              extends PropertyOrdering
case object AscNullsFirst     extends PropertyOrdering
case object DescNullsFirst    extends PropertyOrdering
case object AscNullsLast      extends PropertyOrdering
case object DescNullsLast     extends PropertyOrdering

final case class GroupBy(query: Ast, alias: Ident, body: Ast) extends Query {
  override lazy val quat: Quat     = Quat.Tuple(body.quat, query.quat)
  override lazy val bestQuat: Quat = Quat.Tuple(body.bestQuat, query.bestQuat)
}

final case class GroupByMap(query: Ast, byAlias: Ident, byBody: Ast, mapAlias: Ident, mapBody: Ast) extends Query {
  override def quat: Quat     = mapBody.quat
  override def bestQuat: Quat = mapBody.bestQuat
}

final case class Aggregation(operator: AggregationOperator, ast: Ast) extends Query {
  override lazy val quat: Quat =
    operator match {
      case AggregationOperator.`min`  => ast.quat
      case AggregationOperator.`max`  => ast.quat
      case AggregationOperator.`avg`  => Quat.Value
      case AggregationOperator.`sum`  => Quat.Value
      case AggregationOperator.`size` => Quat.Value
    }
  override lazy val bestQuat: Quat =
    operator match {
      case AggregationOperator.`min`  => ast.bestQuat
      case AggregationOperator.`max`  => ast.bestQuat
      case AggregationOperator.`avg`  => Quat.Value
      case AggregationOperator.`sum`  => Quat.Value
      case AggregationOperator.`size` => Quat.Value
    }
}

final case class Take(query: Ast, n: Ast) extends Query {
  override def quat: Quat     = query.quat
  override def bestQuat: Quat = query.bestQuat
}

final case class Drop(query: Ast, n: Ast) extends Query {
  override def quat: Quat     = query.quat
  override def bestQuat: Quat = query.bestQuat
}

final case class Union(a: Ast, b: Ast) extends Query {
  override def quat: Quat     = a.quat
  override def bestQuat: Quat = a.bestQuat
} // a and b quats should be same

final case class UnionAll(a: Ast, b: Ast) extends Query {
  override def quat: Quat     = a.quat
  override def bestQuat: Quat = a.bestQuat
} // a and b quats should be same

final case class Join(
  typ: JoinType,
  a: Ast,
  b: Ast,
  aliasA: Ident,
  aliasB: Ident,
  on: Ast
) extends Query {
  override lazy val quat: Quat     = Quat.Tuple(a.quat, b.quat)
  override lazy val bestQuat: Quat = Quat.Tuple(a.bestQuat, b.bestQuat)
}

final case class FlatJoin(typ: JoinType, a: Ast, aliasA: Ident, on: Ast) extends Query {
  override def quat: Quat     = a.quat
  override def bestQuat: Quat = a.bestQuat
}

final case class Distinct(a: Ast) extends Query {
  override def quat: Quat     = a.quat
  override def bestQuat: Quat = a.bestQuat
}

final case class DistinctOn(query: Ast, alias: Ident, body: Ast) extends Query {
  override def quat: Quat     = query.quat
  override def bestQuat: Quat = query.bestQuat
}

final case class Nested(a: Ast) extends Query {
  override def quat: Quat     = a.quat
  override def bestQuat: Quat = a.bestQuat
}

//************************************************************

final class Infix(val parts: List[String], val params: List[Ast], val pure: Boolean, val transparent: Boolean)(
  theQuat: => Quat
) extends Ast {
  override lazy val quat: Quat = theQuat
  override def bestQuat: Quat  = quat
  private val id               = Infix.Id(parts, params, pure, transparent)

  def asTransparent = new Infix(this.parts, this.params, this.pure, true)(quat)

  override def equals(that: Any): Boolean =
    that match {
      case p: Infix => this.id == p.id
      case _        => false
    }

  override def hashCode: Int = id.hashCode()
  def copy(
    parts: List[String] = this.parts,
    params: List[Ast] = this.params,
    pure: Boolean = this.pure,
    transparent: Boolean = this.transparent,
    quat: Quat = this.quat
  ): Infix =
    Infix(parts, params, pure, transparent, quat)
}
object Infix {
  private final case class Id(
    parts: List[String],
    params: List[Ast],
    pure: Boolean,
    transparent: Boolean
  )

  def apply(parts: List[String], params: List[Ast], pure: Boolean, transparent: Boolean, quat: => Quat) =
    new Infix(parts, params, pure, transparent)(quat)

  def unapply(i: Infix): Option[(List[String], List[Ast], Boolean, Boolean, Quat)] =
    Some((i.parts, i.params, i.pure, i.transparent, i.quat))
}

final case class Function(params: List[Ident], body: Ast) extends Ast {
  override def quat: Quat     = body.quat
  override def bestQuat: Quat = body.bestQuat
}

final class Ident private (val name: String)(theQuat: => Quat)(val visibility: Visibility) extends Terminal with Ast {
  override lazy val quat: Quat = theQuat
  override def bestQuat: Quat  = quat

  private val id = Ident.Id(name)

  override def equals(that: Any): Boolean =
    that match {
      case p: Ident => this.id == p.id
      case _        => false
    }

  override def hashCode: Int = id.hashCode()

  override def withQuat(quat: => Quat): Ident =
    Ident.Opinionated(this.name, quat, this.visibility)

  // need to define a copy which will propagate current value of visibility into the copy
  def copy(name: String = this.name, quat: => Quat = this.quat): Ident =
    Ident.Opinionated(name, quat, this.visibility)
}

/**
 * Ident represents a single variable name, this typically refers to a table but
 * not always. Invisible identities are a rare case where a user returns an
 * embedded table from a map clause:
 *
 * <pre><code> case class Emb(id: Int, name: String) case class Parent(id: Int,
 * name: String, emb: Emb) case class GrandParent(id: Int, par: Parent)
 *
 * query[GrandParent] .map(g => g.par).distinct .map(p => (p.name,
 * p.emb)).distinct .map(tup => (tup._1, tup._2)).distinct } </code></pre>
 *
 * In these situations, the identity whose properties need to be expanded in the
 * ExpandNestedQueries phase, needs to be marked invisible.
 */
object Ident {
  private final case class Id(name: String)
  def apply(name: String, quat: => Quat = Quat.Value) = new Ident(name)(quat)(Visibility.Visible)
  def unapply(p: Ident): Option[(String, Quat)]       = Some((p.name, p.quat))

  object Opinionated {
    def apply(name: String, quatNew: => Quat, visibilityNew: Visibility) =
      new Ident(name)(quatNew)(visibilityNew)
    def unapply(p: Ident): Option[(String, Quat, Visibility)] =
      Some((p.name, p.quat, p.visibility))
  }
}

// Like identity but is but defined in a clause external to the query. Currently this is used
// for 'returning' clauses to define properties being returned.
final class ExternalIdent private (val name: String)(theQuat: => Quat)(val renameable: Renameable) extends Ast {
  override lazy val quat: Quat = theQuat
  override def bestQuat: Quat  = quat
  private val id               = ExternalIdent.Id(name)

  override def equals(that: Any): Boolean =
    that match {
      case e: ExternalIdent => this.id == e.id
      case _                => false
    }

  override def hashCode: Int = id.hashCode()

  // need to define a copy which will propagate current value of visibility into the copy
  def copy(name: String = this.name, quat: => Quat = this.quat): ExternalIdent =
    ExternalIdent.Opinionated(name, quat, this.renameable)
}

object ExternalIdent {
  private final case class Id(name: String)

  def apply(name: String, quat: => Quat)                = new ExternalIdent(name)(quat)(Renameable.neutral)
  def unapply(e: ExternalIdent): Option[(String, Quat)] = Some((e.name, e.quat))

  object Opinionated {
    def apply(name: String, quat: => Quat, rename: Renameable) =
      new ExternalIdent(name)(quat)(rename)

    def unapply(e: ExternalIdent): Option[(String, Quat, Renameable)] = Some((e.name, e.quat, e.renameable))
  }
}

/**
 * An Opinion represents a piece of data that needs to be propagated through AST
 * transformations but is not directly related to how ASTs are transformed in
 * most stages. For instance, `Renameable` controls how columns are named (i.e.
 * whether to use a `NamingStrategy` or not) after most of the SQL
 * transformations are done. Some transformations (e.g. `RenameProperties` will
 * use `Opinions` or even modify them so that the correct kind of query comes
 * out at the end of the normalizations. That said, Opinions should be
 * transparent in most steps of the normalization.
 */
sealed trait Opinion[T]
sealed trait OpinionValues[T <: Opinion[T]] {
  def neutral: T
}

sealed trait Visibility extends Opinion[Visibility]
object Visibility extends OpinionValues[Visibility] {
  case object Visible extends Visibility with Opinion[Visibility]
  case object Hidden  extends Visibility with Opinion[Visibility]

  override def neutral: Visibility = Visible
}

sealed trait Renameable extends Opinion[Renameable] {
  def fixedOr[T](plain: => T)(otherwise: => T): T =
    this match {
      case Renameable.Fixed => plain
      case _                => otherwise
    }
}

object Renameable extends OpinionValues[Renameable] {
  case object Fixed      extends Renameable with Opinion[Renameable]
  case object ByStrategy extends Renameable with Opinion[Renameable]

  override def neutral: Renameable = ByStrategy
}

/**
 * Properties generally represent column selection from a table or invocation of
 * some kind of method from some other object. Typically, something like
 * <pre>`SELECT p.name FROM People p`</pre> comes from something like
 * <pre>`Map(Entity("People"), Ident("p"), Property(Ident(p), "name"))`</pre>
 * Properties also have an Opinion about how the `NamingStrategy` affects their
 * name. For example something like `Property.Opinionated(Ident(p), "s_name",
 * Fixed)` will become `p.s_name` even if the `NamingStrategy` is `UpperCase`
 * (whereas `Property(Ident(p), "s_name")` would become `p.S_NAME`). When
 * Property is constructed without `Opinionated` being used, the default opinion
 * `ByStrategy` is used.
 */
final class Property(val ast: Ast, val name: String)(val renameable: Renameable, val visibility: Visibility)
    extends Ast {
  override lazy val quat: Quat      = ast.quat.lookup(name, Messages.strictQuatChecking)
  override lazy val bestQuat: Quat  = ast.quat.lookup(name, failNonExist = false)
  lazy val prevName: Option[String] = ast.quat.beforeRenamed(name)

  private val id = Property.Id(ast, name)

  def copy(ast: Ast = this.ast, name: String = this.name): Property =
    Property.Opinionated(ast, name, this.renameable, this.visibility)

  def copyAll(
    ast: Ast = this.ast,
    name: String = this.name,
    renameable: Renameable = this.renameable,
    visibility: Visibility = this.visibility
  ): Property =
    Property.Opinionated(ast, name, renameable, visibility)

  override def equals(that: Any): Boolean =
    that match {
      case e: Property => this.id == e.id
      case _           => false
    }

  override def hashCode: Int = id.hashCode()
}

object Property {
  final case class Id(ast: Ast, name: String)

  // Properties that are 'Hidden' are used for embedded objects whose path should not be expressed
  // during SQL Tokenization.
  def apply(ast: Ast, name: String): Property     = new Property(ast, name)(Renameable.neutral, Visibility.Visible)
  def unapply(p: Property): Option[(Ast, String)] = Some((p.ast, p.name))

  object Opinionated {
    def apply(
      ast: Ast,
      name: String,
      renameableNew: Renameable,
      visibilityNew: Visibility
    ) = new Property(ast, name)(renameableNew, visibilityNew)

    def unapply(p: Property): Option[(Ast, String, Renameable, Visibility)] =
      Some((p.ast, p.name, p.renameable, p.visibility))
  }
}

sealed trait OptionOperation extends Ast
final case class OptionFlatten(ast: Ast) extends OptionOperation {
  override def quat: Quat = ast.quat; override def bestQuat: Quat = ast.bestQuat
}
final case class OptionGetOrElse(ast: Ast, body: Ast) extends OptionOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class OptionOrElse(ast: Ast, body: Ast) extends OptionOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class OptionFlatMap(ast: Ast, alias: Ident, body: Ast) extends OptionOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class OptionMap(ast: Ast, alias: Ident, body: Ast) extends OptionOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class OptionForall(ast: Ast, alias: Ident, body: Ast) extends OptionOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class OptionExists(ast: Ast, alias: Ident, body: Ast) extends OptionOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class OptionContains(ast: Ast, body: Ast) extends OptionOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class OptionIsEmpty(ast: Ast) extends OptionOperation {
  override def quat: Quat = ast.quat; override def bestQuat: Quat = ast.bestQuat
}
final case class OptionNonEmpty(ast: Ast) extends OptionOperation {
  override def quat: Quat = ast.quat; override def bestQuat: Quat = ast.bestQuat
}
final case class OptionIsDefined(ast: Ast) extends OptionOperation {
  override def quat: Quat = ast.quat; override def bestQuat: Quat = ast.bestQuat
}
final case class OptionTableFlatMap(ast: Ast, alias: Ident, body: Ast) extends OptionOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class OptionTableMap(ast: Ast, alias: Ident, body: Ast) extends OptionOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class OptionTableExists(ast: Ast, alias: Ident, body: Ast) extends OptionOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class OptionTableForall(ast: Ast, alias: Ident, body: Ast) extends OptionOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class FilterIfDefined(ast: Ast, alias: Ident, body: Ast) extends OptionOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
case object OptionNoneId
final class OptionNone(theQuat: => Quat) extends OptionOperation with Terminal {
  override lazy val quat: Quat = theQuat
  override def bestQuat: Quat  = quat

  override def withQuat(quat: => Quat): OptionNone = this.copy(quat = quat)
  override def equals(obj: Any): Boolean =
    obj match {
      case _: OptionNone => true
      case _             => false
    }
  override def hashCode(): Int                    = OptionNoneId.hashCode()
  def copy(quat: => Quat = this.quat): OptionNone = OptionNone(quat)
}
object OptionNone {
  def apply(quat: => Quat): OptionNone      = new OptionNone(quat)
  def unapply(on: OptionNone): Option[Quat] = Some(on.quat)
}

final case class OptionSome(ast: Ast) extends OptionOperation {
  override def quat: Quat = ast.quat; override def bestQuat: Quat = ast.bestQuat
}
final case class OptionApply(ast: Ast) extends OptionOperation {
  override def quat: Quat = ast.quat; override def bestQuat: Quat = ast.bestQuat
}
final case class OptionOrNull(ast: Ast) extends OptionOperation {
  override def quat: Quat = ast.quat; override def bestQuat: Quat = ast.bestQuat
}
final case class OptionGetOrNull(ast: Ast) extends OptionOperation {
  override def quat: Quat = ast.quat; override def bestQuat: Quat = ast.bestQuat
}

sealed trait IterableOperation extends Ast
final case class MapContains(ast: Ast, body: Ast) extends IterableOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class SetContains(ast: Ast, body: Ast) extends IterableOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}
final case class ListContains(ast: Ast, body: Ast) extends IterableOperation {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}

final case class If(condition: Ast, `then`: Ast, `else`: Ast) extends Ast {
  override def quat: Quat = `then`.quat; override def bestQuat: Quat = `then`.bestQuat
} // then and else clauses should have identical quats

final case class Assignment(alias: Ident, property: Ast, value: Ast) extends Ast {
  override def quat: Quat = Quat.Value; override def bestQuat: Quat = quat
}
final case class AssignmentDual(alias1: Ident, alias2: Ident, property: Ast, value: Ast) extends Ast {
  override def quat: Quat = Quat.Value; override def bestQuat: Quat = quat
}

//************************************************************

sealed trait Operation extends Ast

final case class UnaryOperation(operator: UnaryOperator, ast: Ast) extends Operation {
  override def quat: Quat = Quat.BooleanExpression; override def bestQuat: Quat = quat
}

final case class BinaryOperation(a: Ast, operator: BinaryOperator, b: Ast) extends Operation {
  import BooleanOperator._
  import NumericOperator._
  import SetOperator.`contains`
  import StringOperator.`startsWith`

  override lazy val quat: Quat = operator match {
    case EqualityOperator.`_==` | EqualityOperator.`_!=` | `&&` | `||` | `>` | `>=` | `<` | `<=` | `startsWith` |
        `contains` =>
      Quat.BooleanExpression
    case _ =>
      Quat.Value
  }

  override def bestQuat: Quat = quat
}
final case class FunctionApply(function: Ast, values: List[Ast]) extends Operation {
  override def quat: Quat = function.quat; override def bestQuat: Quat = function.bestQuat
}

//************************************************************

sealed trait Value extends Ast

final class Constant(val v: Any)(theQuat: => Quat) extends Value {
  override lazy val quat: Quat = theQuat
  override def bestQuat: Quat  = quat

  private val id               = Constant.Id(v)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: Constant => e.id == this.id
      case _           => false
    }
}

object Constant {
  private final case class Id(v: Any)

  def apply(v: Any, quat: => Quat): Constant        = new Constant(v)(quat)
  def unapply(const: Constant): Option[(Any, Quat)] = Some((const.v, const.quat))

  def auto(v: Any): Constant = {
    lazy val theQuat = if (v.isInstanceOf[Boolean]) Quat.BooleanValue else Quat.Value
    new Constant(v)(theQuat)
  }
}

case object NullValue extends Value { override def quat: Quat = Quat.Null; override def bestQuat: Quat = quat }

final case class Tuple(values: List[Ast]) extends Value {
  override lazy val quat: Quat     = Quat.Tuple(values.map(_.quat))
  override lazy val bestQuat: Quat = Quat.Tuple(values.map(_.bestQuat))
}

final case class CaseClass(name: String, values: List[(String, Ast)]) extends Value {
  override lazy val quat: Quat     = Quat.Product(name, values.map { case (k, v) => (k, v.quat) })
  override lazy val bestQuat: Quat = Quat.Product(name, values.map { case (k, v) => (k, v.bestQuat) })
  private val id                   = CaseClass.Id(values)

  // Even thought name is not an "opinion" it still does not make sense to use it to compare CaseClass Asts
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case cc: CaseClass => this.id.equals(cc.id)
      case _             => false
    }
}

object CaseClass {
  final case class Id(values: List[(String, Ast)])
  val GeneratedName = "<Generated>"
  object Single {
    def apply(tup: (String, Ast)) = new CaseClass(GeneratedName, List(tup))
    def unapply(cc: CaseClass): Option[(String, Ast)] =
      cc.values match {
        case (name, property) :: Nil => Some((name, property))
        case _                       => None
      }
  }
}

//************************************************************

final case class Block(statements: List[Ast]) extends Ast {
  override lazy val quat: Quat     = statements.last.quat
  override lazy val bestQuat: Quat = statements.last.bestQuat
} // Note. Assuming Block is not Empty

final case class Val(name: Ident, body: Ast) extends Ast {
  override def quat: Quat     = body.quat
  override def bestQuat: Quat = body.bestQuat
}

//************************************************************

sealed trait Action extends Ast

// Note, technically return type of Actions for most Actions is a Int value but Quat here is used for Returning Quat types
final case class Update(query: Ast, assignments: List[Assignment]) extends Action {
  override def quat: Quat = query.quat; override def bestQuat: Quat = query.bestQuat
}
final case class Insert(query: Ast, assignments: List[Assignment]) extends Action {
  override def quat: Quat = query.quat; override def bestQuat: Quat = query.bestQuat
}
final case class Delete(query: Ast) extends Action {
  override def quat: Quat = query.quat; override def bestQuat: Quat = query.bestQuat
}

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
final case class Returning(action: Ast, alias: Ident, property: Ast) extends ReturningAction {
  override def quat: Quat = property.quat; override def bestQuat: Quat = property.bestQuat
}
final case class ReturningGenerated(action: Ast, alias: Ident, property: Ast) extends ReturningAction {
  override def quat: Quat = property.quat; override def bestQuat: Quat = property.bestQuat
}

final case class Foreach(query: Ast, alias: Ident, body: Ast) extends Action {
  override def quat: Quat = body.quat; override def bestQuat: Quat = body.bestQuat
}

final case class OnConflict(
  insert: Ast,
  target: OnConflict.Target,
  action: OnConflict.Action
) extends Action { override def quat: Quat = insert.quat; override def bestQuat: Quat = insert.bestQuat }

object OnConflict {

  final case class Excluded(alias: Ident) extends Ast {
    override def quat: Quat     = alias.quat
    override def bestQuat: Quat = alias.bestQuat
  }
  final case class Existing(alias: Ident) extends Ast {
    override def quat: Quat     = alias.quat
    override def bestQuat: Quat = alias.bestQuat
  }

  sealed trait Target
  case object NoTarget                               extends Target
  final case class Properties(props: List[Property]) extends Target

  sealed trait Action
  case object Ignore                                         extends Action
  final case class Update(assignments: List[AssignmentDual]) extends Action
}
//************************************************************

/** For Dynamic Infix Splices */
final class Dynamic(val tree: Any)(theQuat: => Quat) extends Ast {
  override lazy val quat: Quat = theQuat
  override def bestQuat: Quat  = quat
}

object Dynamic {
  def apply(tree: Any, quat: => Quat): Dynamic = new Dynamic(tree)(quat)
  def unapply(d: Dynamic): Option[(Any, Quat)] = Some((d.tree, d.quat))

  def apply(v: Any): Dynamic = {
    lazy val theQuat = if (v.isInstanceOf[Boolean]) Quat.BooleanValue else Quat.Value
    new Dynamic(v)(theQuat)
  }
}

final case class QuotedReference(tree: Any, ast: Ast) extends Ast {
  override def quat: Quat = ast.quat; override def bestQuat: Quat = ast.bestQuat;
}

sealed trait External extends Ast
object External {
  sealed trait Source
  object Source {
    final case class UnparsedProperty(name: String) extends Source
    case object Parser                              extends Source
  }
}

/**
 * ********************************************************************
 */
/*                      Only Quill 2                                   */
/**
 * ********************************************************************
 */

sealed trait Lift extends External with Terminal {
  def name: String
  def value: Any
}

sealed trait ScalarLift extends Lift with Terminal {
  def encoder: Any
}

final class ScalarValueLift(val name: String, val source: External.Source, val value: Any, val encoder: Any)(
  theQuat: => Quat
) extends ScalarLift {
  override lazy val quat: Quat                          = theQuat
  override def bestQuat: Quat                           = quat
  override def withQuat(quat: => Quat): ScalarValueLift = this.copy(quat = quat)

  private val id               = ScalarValueLift.Id(name, source, value, encoder)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: ScalarValueLift => e.id == this.id
      case _                  => false
    }
  def copy(
    name: String = this.name,
    source: External.Source = this.source,
    value: Any = this.value,
    encoder: Any = this.encoder,
    quat: => Quat = this.quat
  ): ScalarValueLift =
    ScalarValueLift(name, source, value, encoder, quat)
}
object ScalarValueLift {
  private final case class Id(name: String, source: External.Source, value: Any, encoder: Any)
  def apply(name: String, source: External.Source, value: Any, encoder: Any, quat: => Quat): ScalarValueLift =
    new ScalarValueLift(name, source, value, encoder)(quat)
  def unapply(svl: ScalarValueLift): Option[(String, External.Source, Any, Any, Quat)] =
    Some((svl.name, svl.source, svl.value, svl.encoder, svl.quat))
}

final class ScalarQueryLift(val name: String, val value: Any, val encoder: Any)(theQuat: => Quat) extends ScalarLift {
  override lazy val quat: Quat                          = theQuat
  override def bestQuat: Quat                           = quat
  override def withQuat(quat: => Quat): ScalarQueryLift = this.copy(quat = quat)

  private val id               = ScalarQueryLift.Id(name, value, encoder)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: ScalarQueryLift => e.id == this.id
      case _                  => false
    }

  def copy(
    name: String = this.name,
    value: Any = this.value,
    encoder: Any = this.encoder,
    quat: => Quat = this.quat
  ): ScalarQueryLift =
    ScalarQueryLift(name, value, encoder, quat)
}
object ScalarQueryLift {
  private final case class Id(name: String, value: Any, encoder: Any)
  def apply(name: String, value: Any, encoder: Any, quat: => Quat): ScalarQueryLift =
    new ScalarQueryLift(name, value, encoder)(quat)
  def unapply(l: ScalarQueryLift): Option[(String, Any, Any, Quat)] = Some((l.name, l.value, l.encoder, l.quat))
}

sealed trait CaseClassLift extends Lift

final class CaseClassValueLift(val name: String, val simpleName: String, val value: Any)(theQuat: => Quat)
    extends CaseClassLift {
  override lazy val quat: Quat                             = theQuat
  override def bestQuat: Quat                              = quat
  override def withQuat(quat: => Quat): CaseClassValueLift = this.copy(quat = quat)

  private val id               = CaseClassValueLift.Id(name, simpleName, value)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: CaseClassValueLift => e.id == this.id
      case _                     => false
    }
  def copy(
    name: String = this.name,
    simpleName: String = this.simpleName,
    value: Any = this.value,
    quat: => Quat = this.quat
  ): CaseClassValueLift =
    CaseClassValueLift(name, simpleName, value, quat)
}
object CaseClassValueLift {
  private final case class Id(name: String, simpleName: String, value: Any)
  def apply(name: String, simpleName: String, value: Any, quat: => Quat): CaseClassValueLift =
    new CaseClassValueLift(name, simpleName, value)(quat)
  def unapply(l: CaseClassValueLift): Option[(String, String, Any, Quat)] =
    Some((l.name, l.simpleName, l.value, l.quat))
}

final class CaseClassQueryLift(val name: String, val value: Any)(theQuat: => Quat) extends CaseClassLift {
  override lazy val quat: Quat                             = theQuat
  override def bestQuat: Quat                              = quat
  override def withQuat(quat: => Quat): CaseClassQueryLift = this.copy(quat = quat)

  private val id               = CaseClassQueryLift.Id(name, value)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: CaseClassQueryLift => e.id == this.id
      case _                     => false
    }

  def copy(name: String = this.name, value: Any = this.value, quat: => Quat = this.quat): CaseClassQueryLift =
    CaseClassQueryLift(name, value, quat)
}

object CaseClassQueryLift {
  private final case class Id(name: String, value: Any)
  def apply(name: String, value: Any, quat: => Quat): CaseClassQueryLift = new CaseClassQueryLift(name, value)(quat)
  def unapply(l: CaseClassQueryLift): Option[(String, Any, Quat)]        = Some((l.name, l.value, l.quat))
}

/**
 * ********************************************************************
 */
/*                      New for ProtoQuill                             */
/**
 * ********************************************************************
 */

sealed trait Tag extends External {
  def uid: String
}

final case class ScalarTagId(uid: String)
final case class ScalarTag(uid: String, source: External.Source) extends Tag {
  override def quat: Quat     = Quat.Value
  override def bestQuat: Quat = quat

  private val id               = ScalarTagId(uid)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: ScalarTag => this.id == e.id
      case _            => false
    }
}

final case class QuotationTagId(uid: String)
final case class QuotationTag(uid: String) extends Tag {
  override def quat: Quat     = Quat.Value
  override def bestQuat: Quat = quat

  private val id               = QuotationTagId(uid)
  override def hashCode(): Int = id.hashCode()
  override def equals(obj: Any): Boolean =
    obj match {
      case e: QuotationTag => this.id == e.id
      case _               => false
    }
}
