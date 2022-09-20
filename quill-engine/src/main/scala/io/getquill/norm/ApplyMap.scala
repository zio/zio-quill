package io.getquill.norm

import io.getquill.ast._
import io.getquill.quat.Quat
import io.getquill.util.{Interpolator, TraceConfig}
import io.getquill.util.Messages.TraceType
import io.getquill.sql.Common.ContainsImpurities

/**
 * Notes for the conceptual examples below. Gin and Tonic were used as
 * prototypical examples of things that "are joined". In the table form, they
 * are aliased to the following tonics is Query[Tonic], tonic is Tonic gins is
 * Query[Gin], is Gin waters is Query[Water], water is Water
 *
 * ginifySpirit is some f:Spirit => Gin tonicfyWater is some f:Tonic => Water
 * bottleGin is some f:Gin => Bottle Additionally Map(a,b,c).quat is the same as
 * c.quat. The former is used in most examples with DetachableMap
 */
class ApplyMap(traceConfig: TraceConfig) {

  val interp = new Interpolator(TraceType.ApplyMap, traceConfig, 3)
  import interp._

  // Note, since the purpose of this beta reduction is to check isomophism types should not actually be
  // checked here since they may be wrong (i.e. if there is no actual isomorphism).
  private def isomorphic(e: Ast, c: Ast, alias: Ident) =
    BetaReduction(e, TypeBehavior.ReplaceWithReduction, alias -> c) == c

  object InfixedTailOperation {

    def hasImpureInfix(ast: Ast) =
      CollectAst(ast) { case i @ Infix(_, _, false, _, _) =>
        i
      }.nonEmpty

    def unapply(ast: Ast): Option[Ast] =
      ast match {
        case cc: CaseClass if hasImpureInfix(cc)     => Some(cc)
        case tup: Tuple if hasImpureInfix(tup)       => Some(tup)
        case p: Property if hasImpureInfix(p)        => Some(p)
        case b: BinaryOperation if hasImpureInfix(b) => Some(b)
        case u: UnaryOperation if hasImpureInfix(u)  => Some(u)
        case i @ Infix(_, _, false, _, _)            => Some(i)
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

  // What kind of map's can't have things inside of them extracted.
  // For example:
  //   query[Person].map(p => p.name).filter(n => n == "Joe")
  //   (Given by the roughly the AST: Filter(Map(query[Person],p,p.name),n,n == "Joe")
  //   (whose query is highly inefficient: SELECT n.name FROM (SELECT p.name FROM Person) AS n WHERE n.name == 'Joe'
  // ...can typically be replaced with:
  //   query[Person].filter(p => p.name == "Joe").map(p => p.name)
  //   (whose query is much better: SELECT p.name FROM Person where p.name == 'Joe')
  // However if there's a groupBy after the Map or an aggregation e.g:
  //   query[Person].map(p => (p.name, p.age)).groupBy(t => t._1)...
  //   query[Person].map(p => max(p.age))...
  // then you cannot do this re-arrangement. DetachableMap
  // does the determination of which one can and cannot be re-arranged by returning None or Some
  // for these cases respectively.
  object DetachableMap {
    def unapply(ast: Ast): Option[(Ast, Ident, Ast)] =
      ast match {
        // Maps that contains
        case Map(_, _, ContainsImpurities())    => None
        case Map(a: GroupBy, b, c)              => None
        case Map(a: DistinctOn, b, c)           => None
        case Map(a: FlatJoin, b, c)             => None // FlatJoin should always be surrounded by a Map
        case Map(a, b, InfixedTailOperation(c)) => None
        case Map(a, b, c)                       => Some((a, b, c))
        case _                                  => None
      }
  }

  def unapply(q: Query): Option[Query] =
    q match {

      case Map(a: GroupBy, b, c) if (b == c)    => None
      case Map(a: GroupByMap, b, c) if (b == c) => None
      case Map(a: Nested, b, c) if (b == c)     => None
      case Map(a: FlatJoin, b, c) if (b == c)   => None // FlatJoin should always be surrounded by a Map
      case Nested(DetachableMap(a, b, c))       => None

      //  map(i => (i.i, i.l)).distinct.map(x => (x._1, x._2)) =>
      //    map(i => (i.i, i.l)).distinct
      case Map(Distinct(DetachableMap(a, b, c)), d, e) if isomorphic(e, c, d) =>
        trace"ApplyMap on Distinct for $q" andReturn Some(Distinct(Map(a, b, c)))

      // a.map(b => c).map(d => e) =>
      //    a.map(b => e[d := c])
      case before @ Map(MapWithoutInfixes(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        trace"ApplyMap on double-map for $q" andReturn Some(Map(a, b, er))

      // a.map(b => b) =>
      //    a
      case Map(a: Query, b, c) if (b == c) =>
        trace"ApplyMap on identity-map for $q" andReturn Some(a)

      // a.map(b => c).flatMap(d => e) =>
      //    a.flatMap(b => e[d := c])
      case FlatMap(DetachableMap(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        trace"ApplyMap inside flatMap for $q" andReturn Some(FlatMap(a, b, er))

      // a.map(b => c).filter(d => e) =>
      //    a.filter(b => e[d := c]).map(b => c)
      case Filter(DetachableMap(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> c)
        trace"ApplyMap inside filter for $q" andReturn Some(Map(Filter(a, b, er), b, c))

      // a.map(b => c).sortBy(d => e) =>
      //    a.sortBy(b => e[d := c]).map(b => c)
      case SortBy(DetachableMap(a, b, c), d, e, f) =>
        val er = BetaReduction(e, d -> c)
        trace"ApplyMap inside sortBy for $q" andReturn Some(Map(SortBy(a, b, er, f), b, c))

      // a.map(b => c).sortBy(d => e).distinct =>
      //    a.sortBy(b => e[d := c]).map(b => c).distinct
      case SortBy(Distinct(DetachableMap(a, b, c)), d, e, f) =>
        val er = BetaReduction(e, d -> c)
        trace"ApplyMap inside sortBy+distinct for $q" andReturn Some(Distinct(Map(SortBy(a, b, er, f), b, c)))

      // === Conceptual Example ===
      // Instead of transforming spirit into gin and the bottling the gin, bottle the
      // spirit first, then have the spirit transform into gin inside of the bottles.
      //
      // spirits.map(spirit => ginifySpirit).groupBy(gin => bottleGin) =>
      //    spirits.groupBy(spirit => bottleGin[gin := ginifySprit]).map(x: Tuple[(Bottle, Spirit)] => (x._1, x._2.map(spirit => ginifySpirit))) :Tuple[(Bottle, Gin)]

      // a.map(b => c).groupBy(d => e) =>
      //    a.groupBy(b => e[d := c]).map(x => (x._1, x._2.map(b => c)))
      // where: x._2.map(b => c).type == d.type
      case GroupBy(DetachableMap(a, b, c), d, e) =>
        val er  = BetaReduction(e, d -> c)
        val grp = GroupBy(a, b, er)
        // Use grp.quat directly instead of trying to compute it manually. Before it was Quat.Tuple(e.quat, c.quat)
        // which could produce subtle bugs in some cases e.g. CC(_1:V,_2:V) instead of CC(_1:V,_2:CC(_1:V))
        // if you're reducing Grp(M(quat:CC(_1:V),_,_), quat:CC(_1:V,_2:CC(_1:V))) it would become M(Grp(...):,id:Id(quat:CC(_1:V,_2:V)),_)
        // and the quat of the `id` is CC(_1:V,_2:V) instead of CC(_1:V,_2:CC(_1:V))). It would break beta reductions in later phases.
        val x    = Ident("x", grp.quat)
        val x1   = Property(x, "_1")
        val x2   = Property(x, "_2")
        val body = Tuple(List(x1, Map(x2, b, c)))
        trace"ApplyMap inside groupBy for $q" andReturn Some(Map(grp, x, body))

      // === Conceptual Example (same as for groupBy.map) ===
      // Instead of transforming spirit into gin and the bottling the gin, bottle the
      // spirit first, then have the spirit transform into gin inside of the bottles.
      // (The only difference between this and groupByMap is that we have two kinds of bottles: A and B)
      //
      // spirits.map(spirit => ginifySpirit).groupByMap(gin => bottleGinA)(gin => bottleGinB) =>
      //    spirits.groupByMap(spirit => bottleGinA[gin := ginifySpirit])(spirit => bottleGinB[gin := ginifySpirit])

      // a.map(b => c).groupByMap(d => e)(d => f) =>
      //    a.groupByMap(b => e[d := c])(b => f[d := c])
      // where d := d1
      case GroupByMap(DetachableMap(a, b, c), d, e, d1, f) =>
        val er  = BetaReduction(e, d -> c)
        val fr  = BetaReduction(f, d1 -> c)
        val grp = GroupByMap(a, b, er, b, fr)
        trace"ApplyMap inside groupByMap for $q" andReturn Some(grp)

      // a.map(b => c).drop(d) =>
      //    a.drop(d).map(b => c)
      case Drop(DetachableMap(a, b, c), d) =>
        trace"ApplyMap inside drop for $q" andReturn Some(Map(Drop(a, d), b, c))

      // a.map(b => c).take(d) =>
      //    a.drop(d).map(b => c)
      case Take(DetachableMap(a, b, c), d) =>
        trace"ApplyMap inside take for $q" andReturn Some(Map(Take(a, d), b, c))

      // a.map(b => c).nested =>
      //    a.nested.map(b => c)
      case Nested(DetachableMap(a, b, c)) =>
        trace"ApplyMap inside nested for $q" andReturn Some(Map(Nested(a), b, c))

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
        val t   = Ident("t", Quat.Tuple(b.quat, e.quat))
        val t1  = BetaReduction(c, b -> Property(t, "_1"))
        val t2  = BetaReduction(f, e -> Property(t, "_2"))
        trace"ApplyMap inside join-reduceDouble for $q" andReturn
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
        val t   = Ident("t", Quat.Tuple(a.quat, c.quat))
        val t1  = Property(t, "_1")
        val t2  = BetaReduction(d, c -> Property(t, "_2"))
        trace"ApplyMap inside join-reduceRight for $q" andReturn
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
        val t   = Ident("t", Quat.Tuple(b.quat, d.quat))
        val t1  = BetaReduction(c, b -> Property(t, "_1"))
        val t2  = Property(t, "_2")
        trace"ApplyMap inside join-reduceLeft for $q" andReturn
          Some(Map(Join(tpe, a, d, b, iB, onr), t, Tuple(List(t1, t2))))

      case other => None
    }
}
