package io.getquill.norm

import io.getquill.ast._
import io.getquill.quat.Quat

/**
 * Notes for the conceptual examples below. Gin and Tonic were used as prototypical
 * examples of things that "are joined". In the table form, they are alude to the following
 * tonics is Query[Tonic], tonic is Tonic
 * gins is Query[Gin], is Gin
 * waters is Query[Water], water is Water
 *
 * ginifySpirit is some f:Spirit => Gin
 * tonicfyWater is some f:Tonic => Water
 * bottleGin is some f:Gin => Bottle
 * Additionally Map(a,b,c).quat is the same as c.quat. The former
 * is used in most examples with DetachableMap
 */
object ApplyMap {

  // Note, since the purpose of this beta reduction is to check isomophism types should not actually be
  // checked here since they may be wrong (i.e. if there is no actual isomorphism).
  private def isomorphic(e: Ast, c: Ast, alias: Ident) =
    BetaReduction(e, TypeBehavior.ReplaceWithReduction, alias -> c) == c

  object InfixedTailOperation {

    def hasImpureInfix(ast: Ast) =
      CollectAst(ast) {
        case i @ Infix(_, _, false, _) => i
      }.nonEmpty

    def unapply(ast: Ast): Option[Ast] =
      ast match {
        case cc: CaseClass if hasImpureInfix(cc)     => Some(cc)
        case tup: Tuple if hasImpureInfix(tup)       => Some(tup)
        case p: Property if hasImpureInfix(p)        => Some(p)
        case b: BinaryOperation if hasImpureInfix(b) => Some(b)
        case u: UnaryOperation if hasImpureInfix(u)  => Some(u)
        case i @ Infix(_, _, false, _)               => Some(i)
        case _                                       => None
      }
  }

  object MapWithoutInfixes {
    def unapply(ast: Ast): Option[(Ast, Ident, Ast)] =
      ast match {
        case Map(a, b, InfixedTailOperation(c)) => None
        case Map(a, b, c)                       => Some((a, b, c))
        case _                                  => None
      }
  }

  object DetachableMap {
    def unapply(ast: Ast): Option[(Ast, Ident, Ast)] =
      ast match {
        case Map(a: GroupBy, b, c)              => None
        case Map(a: FlatJoin, b, c)             => None // FlatJoin should always be surrounded by a Map
        case Map(a, b, InfixedTailOperation(c)) => None
        case Map(a, b, c)                       => Some((a, b, c))
        case _                                  => None
      }
  }

  def unapply(q: Query): Option[Query] =
    q match {

      case Map(a: GroupBy, b, c) if (b == c)    => None
      case Map(a: Nested, b, c) if (b == c)     => None
      case Map(a: FlatJoin, b, c) if (b == c)   => None // FlatJoin should always be surrounded by a Map
      case Nested(DetachableMap(a: Join, b, c)) => None

      //  map(i => (i.i, i.l)).distinct.map(x => (x._1, x._2)) =>
      //    map(i => (i.i, i.l)).distinct
      case Map(Distinct(DetachableMap(a, b, c)), d, e) if isomorphic(e, c, d) =>
        Some(Distinct(Map(a, b, c)))

      // a.map(b => c).map(d => e) =>
      //    a.map(b => e[d := c])
      case before @ Map(MapWithoutInfixes(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        Some(Map(a, b, er))

      // a.map(b => b) =>
      //    a
      case Map(a: Query, b, c) if (b == c) =>
        Some(a)

      // a.map(b => c).flatMap(d => e) =>
      //    a.flatMap(b => e[d := c])
      case FlatMap(DetachableMap(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        Some(FlatMap(a, b, er))

      // a.map(b => c).filter(d => e) =>
      //    a.filter(b => e[d := c]).map(b => c)
      case Filter(DetachableMap(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        Some(Map(Filter(a, b, er), b, c))

      // a.map(b => c).sortBy(d => e) =>
      //    a.sortBy(b => e[d := c]).map(b => c)
      case SortBy(DetachableMap(a, b, c), d, e, f) =>
        val er = BetaReduction(e, d -> c)
        Some(Map(SortBy(a, b, er, f), b, c))

      // a.map(b => c).sortBy(d => e).distinct =>
      //    a.sortBy(b => e[d := c]).map(b => c).distinct
      case SortBy(Distinct(DetachableMap(a, b, c)), d, e, f) =>
        val er = BetaReduction(e, d -> c)
        Some(Distinct(Map(SortBy(a, b, er, f), b, c)))

      // === Conceptual Example ===
      // Instead of transforming spirit into gin and the bottling the join, bottle the
      // spirit first, then have the spirit transform into gin inside of the bottles.
      //
      // spirits.map(spirit => ginifySpirit).groupBy(gin => bottleGin) =>
      //    spirits.groupBy(spirit => bottleGin[gin := ginifySprit]).map(x: Tuple[(Bottle, Spirit)] => (x._1, x._2.map(spirit => ginifySpirit))) :Tuple[(Bottle, Gin)]

      // a.map(b => c).groupBy(d => e) =>
      //    a.groupBy(b => e[d := c]).map(x => (x._1, x._2.map(b => c)))
      // x._2.map(b => c).type == d.type
      case GroupBy(DetachableMap(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        val x = Ident("x", Quat.Tuple(e.quat, c.quat))
        val x1 = Property(x, "_1")
        val x2 = Property(x, "_2")
        val body = Tuple(List(x1, Map(x2, b, c)))
        Some(Map(GroupBy(a, b, er), x, body))

      // a.map(b => c).drop(d) =>
      //    a.drop(d).map(b => c)
      case Drop(DetachableMap(a, b, c), d) =>
        Some(Map(Drop(a, d), b, c))

      // a.map(b => c).take(d) =>
      //    a.drop(d).map(b => c)
      case Take(DetachableMap(a, b, c), d) =>
        Some(Map(Take(a, d), b, c))

      // a.map(b => c).nested =>
      //    a.nested.map(b => c)
      case Nested(DetachableMap(a, b, c)) =>
        Some(Map(Nested(a), b, c))

      // === Conceptual Example ===
      // Instead of combining gin and tonic, pour spirit and water into a cup and transform both
      // the spirit into gin, and the water into tonic inside of the cup.
      //
      // spirits.map(spirit => ginifySpririt).join(waters.map(water => tonicfyWater)).on((gin, tonic) => on)
      //    spirits.join(waters).on((spirit, water) => on[gin := ginifySpirit, tonic := tonicfyWater]).map(t:Tuple[(Gin, Tonic)] => (ginifySpirit[spirit := t._1], tonicfyWater[water := t._2]))

      // a.map(b => c).*join(d.map(e => f)).on((iA, iB) => on)
      //    a.*join(d).on((b, e) => on[iA := c, iB := f]).map(t => (c[b := t._1], f[e := t._2]))
      case Join(tpe, DetachableMap(a, b, c), DetachableMap(d, e, f), iA, iB, on) =>
        val onr = BetaReduction(on, iA -> c, iB -> f)
        val t = Ident("t", Quat.Tuple(b.quat, e.quat))
        val t1 = BetaReduction(c, b -> Property(t, "_1"))
        val t2 = BetaReduction(f, e -> Property(t, "_2"))
        Some(Map(Join(tpe, a, d, b, e, onr), t, Tuple(List(t1, t2))))

      // === Conceptual Example ===
      // Instead of combining gin and tonic, pour gin and water into a cup and transform the water
      // into gin inside of the cup.
      //
      // gins.join(waters.map(water => tonicfyWater).on((gin, tonic) => on)
      //    gins.join(water).on((gin, water) => on[water := tonicfyWater]).map(t:Tuple[(Gin, Water)] => t._1, tonicfyWater[water := t._2]) :Tuple[(Gin, Water)]

      // a.*join(b.map(c => d)).on((iA, iB) => on)
      //    a.*join(b).on((iA, c) => on[iB := d]).map(t => (t._1, d[c := t._2]))
      case Join(tpe, a, DetachableMap(b, c, d), iA, iB, on) =>
        val onr = BetaReduction(on, iB -> d)
        val t = Ident("t", Quat.Tuple(a.quat, c.quat))
        val t1 = Property(t, "_1")
        val t2 = BetaReduction(d, c -> Property(t, "_2"))
        Some(Map(Join(tpe, a, b, iA, c, onr), t, Tuple(List(t1, t2))))

      // === Conceptual Example ===
      // Instead of combining gin and tonic, pour raw spirit with tonic in a cup and transform the spirit
      // inside of the tup into tonic.
      //
      // spirits.map(spirit => ginifySpirit).join(tonics).on((gin, tonic) => on)
      //    spirits.join(tonics).on((spirit, tonic) => on[gin := ginifySpirit]).map(t:Tuple[(Spririt, Tonic)] => (ginifySpirit[spirit := t._1], t._2)) :Tuple[(Gin, Tonic)]

      // a.map(b => c).*join(d).on((iA, iB) => on)
      //    a.*join(d).on((b, iB) => on[iA := c]).map(t => (c[b := t._1], t._2))
      // Quat Equivalence: a.quat == b.quat == iA.quat
      case Join(tpe, DetachableMap(a, b, c), d, iA, iB, on) =>
        val onr = BetaReduction(on, iA -> c)
        val t = Ident("t", Quat.Tuple(b.quat, d.quat))
        val t1 = BetaReduction(c, b -> Property(t, "_1"))
        val t2 = Property(t, "_2")
        Some(Map(Join(tpe, a, d, b, iB, onr), t, Tuple(List(t1, t2))))

      case other => None
    }
}