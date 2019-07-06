package io.getquill.norm.capture

import io.getquill.ast.{ Entity, Filter, FlatJoin, FlatMap, GroupBy, Ident, Join, Map, Query, SortBy, StatefulTransformer, _ }
import io.getquill.norm.{ BetaReduction, Normalize }

private[getquill] case class AvoidAliasConflict(state: collection.Set[Ident])
  extends StatefulTransformer[collection.Set[Ident]] {

  object Unaliased {

    private def isUnaliased(q: Ast): Boolean =
      q match {
        case Nested(q: Query)         => isUnaliased(q)
        case Take(q: Query, _)        => isUnaliased(q)
        case Drop(q: Query, _)        => isUnaliased(q)
        case Aggregation(_, q: Query) => isUnaliased(q)
        case Distinct(q: Query)       => isUnaliased(q)
        case _: Entity | _: Infix     => true
        case _                        => false
      }

    def unapply(q: Ast): Option[Ast] =
      q match {
        case q if (isUnaliased(q)) => Some(q)
        case _                     => None
      }
  }

  override def apply(q: Query): (Query, StatefulTransformer[collection.Set[Ident]]) =
    q match {

      case FlatMap(Unaliased(q), x, p) =>
        apply(x, p)(FlatMap(q, _, _))

      case ConcatMap(Unaliased(q), x, p) =>
        apply(x, p)(ConcatMap(q, _, _))

      case Map(Unaliased(q), x, p) =>
        apply(x, p)(Map(q, _, _))

      case Filter(Unaliased(q), x, p) =>
        apply(x, p)(Filter(q, _, _))

      case SortBy(Unaliased(q), x, p, o) =>
        apply(x, p)(SortBy(q, _, _, o))

      case GroupBy(Unaliased(q), x, p) =>
        apply(x, p)(GroupBy(q, _, _))

      case Join(t, a, b, iA, iB, o) =>
        val (ar, art) = apply(a)
        val (br, brt) = art.apply(b)
        val freshA = freshIdent(iA, brt.state)
        val freshB = freshIdent(iB, brt.state + freshA)
        val or = BetaReduction(o, iA -> freshA, iB -> freshB)
        val (orr, orrt) = AvoidAliasConflict(brt.state + freshA + freshB)(or)
        (Join(t, ar, br, freshA, freshB, orr), orrt)

      case FlatJoin(t, a, iA, o) =>
        val (ar, art) = apply(a)
        val freshA = freshIdent(iA)
        val or = BetaReduction(o, iA -> freshA)
        val (orr, orrt) = AvoidAliasConflict(art.state + freshA)(or)
        (FlatJoin(t, ar, freshA, orr), orrt)

      case _: Entity | _: FlatMap | _: ConcatMap | _: Map | _: Filter | _: SortBy | _: GroupBy |
        _: Aggregation | _: Take | _: Drop | _: Union | _: UnionAll | _: Distinct | _: Nested =>
        super.apply(q)
    }

  private def apply(x: Ident, p: Ast)(f: (Ident, Ast) => Query): (Query, StatefulTransformer[collection.Set[Ident]]) = {
    val fresh = freshIdent(x)
    val pr = BetaReduction(p, x -> fresh)
    val (prr, t) = AvoidAliasConflict(state + fresh)(pr)
    (f(fresh, prr), t)
  }

  private def freshIdent(x: Ident, state: collection.Set[Ident] = state): Ident = {
    def loop(x: Ident, n: Int): Ident = {
      val fresh = Ident(s"${x.name}$n")
      if (!state.contains(fresh))
        fresh
      else
        loop(x, n + 1)
    }
    if (!state.contains(x))
      x
    else
      loop(x, 1)
  }

  /**
   * Sometimes we need to change the variables in a function because they will might conflict with some variable
   * further up in the macro. Right now, this only happens when you do something like this:
   * <code>
   * val q = quote { (v: Foo) => query[Foo].insert(v) }
   * run(q(lift(v)))
   * </code>
   * Since 'v' is used by actionMeta in order to map keys to values for insertion, using it as a function argument
   * messes up the output SQL like so:
   * <code>
   *   INSERT INTO MyTestEntity (s,i,l,o) VALUES (s,i,l,o) instead of (?,?,?,?)
   * </code>
   * Therefore, we need to have a method to remove such conflicting variables from Function ASTs
   */
  private def applyFunction(f: Function): Function = {
    val (newBody, _, newParams) =
      f.params.foldLeft((f.body, state, List[Ident]())) {
        case ((body, state, newParams), param) => {
          val fresh = freshIdent(param)
          val pr = BetaReduction(body, param -> fresh)
          val (prr, t) = AvoidAliasConflict(state + fresh)(pr)
          (prr, t.state, newParams :+ fresh)
        }
      }
    Function(newParams, newBody)
  }

  private def applyForeach(f: Foreach): Foreach = {
    val fresh = freshIdent(f.alias)
    val pr = BetaReduction(f.body, f.alias -> fresh)
    val (prr, _) = AvoidAliasConflict(state + fresh)(pr)
    Foreach(f.query, fresh, prr)
  }
}

private[getquill] object AvoidAliasConflict {

  def apply(q: Query): Query =
    AvoidAliasConflict(collection.Set[Ident]())(q) match {
      case (q, _) => q
    }

  /**
   * Make sure query parameters do not collide with paramters of a AST function. Do this
   * by walkning through the function's subtree and transforming and queries encountered.
   */
  def sanitizeVariables(f: Function, dangerousVariables: Set[Ident]): Function = {
    AvoidAliasConflict(dangerousVariables).applyFunction(f)
  }

  /** Same is `sanitizeVariables` but for Foreach **/
  def sanitizeVariables(f: Foreach, dangerousVariables: Set[Ident]): Foreach = {
    AvoidAliasConflict(dangerousVariables).applyForeach(f)
  }

  def sanitizeQuery(q: Query, dangerousVariables: Set[Ident]): Query = {
    AvoidAliasConflict(dangerousVariables).apply(q) match {
      // Propagate aliasing changes to the rest of the query
      case (q, _) => Normalize(q)
    }
  }
}
