package io.getquill.norm.capture

import io.getquill.ast.{ Entity, Filter, FlatJoin, FlatMap, GroupBy, Ident, Join, Map, Query, SortBy, StatefulTransformer, _ }
import io.getquill.norm.{ BetaReduction, Normalize }
import io.getquill.util.Interpolator
import io.getquill.util.Messages.TraceType

import scala.collection.immutable.Set

private[getquill] case class AvoidAliasConflict(state: Set[Ident])
  extends StatefulTransformer[Set[Ident]] {

  val interp = new Interpolator(TraceType.AvoidAliasConflict, 3)
  import interp._

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

  // Cannot realize direct super-cluase of a join because of how ExpandJoin does $a$b.
  // This is tested in JoinComplexSpec which verifies that ExpandJoin behaves correctly.
  object CanRealias {
    def unapply(q: Ast): Boolean =
      q match {
        case _: Join => false
        case _       => true
      }
  }

  private def recurseAndApply[T <: Query](elem: T)(ext: T => (Ast, Ident, Ast))(f: (Ast, Ident, Ast) => T): (T, StatefulTransformer[Set[Ident]]) =
    trace"Uncapture RecurseAndApply $elem " andReturn {
      val (newElem, newTrans) = super.apply(elem)
      val ((query, alias, body), state) =
        (ext(newElem.asInstanceOf[T]), newTrans.state)

      val fresh = freshIdent(alias)
      val pr =
        trace"RecurseAndApply Replace: $alias -> $fresh: " andReturn
          BetaReduction(body, alias -> fresh)

      (f(query, fresh, pr), AvoidAliasConflict(state + fresh))
    }

  override def apply(qq: Query): (Query, StatefulTransformer[Set[Ident]]) =
    trace"Uncapture $qq " andReturn
      qq match {

        case FlatMap(Unaliased(q), x, p) =>
          apply(x, p)(FlatMap(q, _, _))

        case ConcatMap(Unaliased(q), x, p) =>
          apply(x, p)(ConcatMap(q, _, _))

        case Map(Unaliased(q), x, p) =>
          apply(x, p)(Map(q, _, _))

        case Filter(Unaliased(q), x, p) =>
          apply(x, p)(Filter(q, _, _))

        case GroupBy(Unaliased(q), x, p) =>
          apply(x, p)(GroupBy(q, _, _))

        case m @ FlatMap(CanRealias(), _, _) =>
          recurseAndApply(m)(m => (m.query, m.alias, m.body))(FlatMap(_, _, _))

        case m @ ConcatMap(CanRealias(), _, _) =>
          recurseAndApply(m)(m => (m.query, m.alias, m.body))(ConcatMap(_, _, _))

        case m @ Map(CanRealias(), _, _) =>
          recurseAndApply(m)(m => (m.query, m.alias, m.body))(Map(_, _, _))

        case m @ Filter(CanRealias(), _, _) =>
          recurseAndApply(m)(m => (m.query, m.alias, m.body))(Filter(_, _, _))

        case m @ GroupBy(CanRealias(), _, _) =>
          recurseAndApply(m)(m => (m.query, m.alias, m.body))(GroupBy(_, _, _))

        case SortBy(Unaliased(q), x, p, o) =>
          trace"Unaliased $qq uncapturing $x" andReturn
          apply(x, p)(SortBy(q, _, _, o))

        case Join(t, a, b, iA, iB, o) =>
          trace"Uncapturing Join $qq" andReturn {
            val (ar, art) = apply(a)
            val (br, brt) = art.apply(b)
            val freshA = freshIdent(iA, brt.state)
            val freshB = freshIdent(iB, brt.state + freshA)
            val or =
              trace"Uncapturing Join: Replace $iA -> $freshA, $iB -> $freshB" andReturn
                BetaReduction(o, iA -> freshA, iB -> freshB)
            val (orr, orrt) =
              trace"Uncapturing Join: Recurse with state: ${brt.state} + $freshA + $freshB" andReturn
                AvoidAliasConflict(brt.state + freshA + freshB)(or)

            (Join(t, ar, br, freshA, freshB, orr), orrt)
          }

        case FlatJoin(t, a, iA, o) =>
          trace"Uncapturing FlatJoin $qq" andReturn {
            val (ar, art) = apply(a)
            val freshA = freshIdent(iA)
            val or =
              trace"Uncapturing FlatJoin: Reducing $iA -> $freshA" andReturn
                BetaReduction(o, iA -> freshA)
            val (orr, orrt) =
              trace"Uncapturing FlatJoin: Recurse with state: ${art.state} + $freshA" andReturn
                AvoidAliasConflict(art.state + freshA)(or)

            (FlatJoin(t, ar, freshA, orr), orrt)
          }

        case _: Entity | _: FlatMap | _: ConcatMap | _: Map | _: Filter | _: SortBy | _: GroupBy |
        _: Aggregation | _: Take | _: Drop | _: Union | _: UnionAll | _: Distinct | _: Nested =>
          super.apply(qq)
      }

  private def apply(x: Ident, p: Ast)(f: (Ident, Ast) => Query): (Query, StatefulTransformer[Set[Ident]]) =
    trace"Uncapture Apply ($x, $p)" andReturn {
      val fresh = freshIdent(x)
      val pr =
        trace"Uncapture Apply: $x -> $fresh" andReturn
          BetaReduction(p, x -> fresh)
      val (prr, t) =
        trace"Uncapture Apply Recurse" andReturn
          AvoidAliasConflict(state + fresh)(pr)

      (f(fresh, prr), t)
    }

  private def freshIdent(x: Ident, state: Set[Ident] = state): Ident = {
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
    AvoidAliasConflict(Set[Ident]())(q) match {
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
