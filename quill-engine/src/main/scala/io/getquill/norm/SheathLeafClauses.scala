package io.getquill.norm

import io.getquill.ast.{
  Aggregation,
  Ast,
  CaseClass,
  ConcatMap,
  Distinct,
  Filter,
  FlatMap,
  GroupBy,
  GroupByMap,
  Ident,
  Join,
  Map,
  Property,
  Query,
  StatefulTransformerWithStack,
  Union,
  UnionAll
}
import io.getquill.ast.Ast.LeafQuat
import io.getquill.ast.StatefulTransformerWithStack.History
import io.getquill.util.{Interpolator, TraceConfig}
import io.getquill.util.Messages.TraceType

/**
 * The state produced in some child clause by the `sheathLeaf` function is
 * essentially "consumed" by the `elaborateSheath` function in the parent.
 *
 * Note that in the documentation is use a couple of shorthands:
 *
 * M - means Map Fm - means FlatMap ent - means a Ast Query. Typically just a
 * Ast Entity e.v - this dot-shorthand means Property(e, v) where e is an Ast
 * Ident. This is essentially a scalar-projection from the entity e. leaf -
 * Typically this is a query-ast clause that results in a scalar type. It could
 * be M(ent,e,e.v) or an `sql"stuff".as[Query[Int/String/Boolean/etc...] ]`
 */
case class SheathLeafClauses(state: Option[String], traceConfig: TraceConfig)
    extends StatefulTransformerWithStack[Option[String]] {

  val interp = new Interpolator(TraceType.ShealthLeaf, traceConfig, 3)
  import interp._

  def sheathLeaf(ast: Ast) =
    ast match {
      case LeafQuat(p: Property) => (CaseClass.Single(p.name -> p), Some(p.name))
      case LeafQuat(body)        => (CaseClass.Single("x" -> body), Some("x"))
      case other                 => (other, None)
    }

  def elaborateSheath(ast: Ast)(state: Option[String], e: Ident, newIdent: Ident) =
    state match {
      case Some(v) =>
        val e1  = newIdent
        val ev1 = Property(e1, v)
        // Note that the Quat of e and ev1 should be the same for example, if e is Quat.Value then
        // e1 should be Quat.CaseClass("v"->Quat.Value) and ev1 selects "v" from that which is again, Quat.Value
        // (have a look at how `Property` computes it's Quat for info on that)
        trace"Elaborate Sheath. Replace ${e} with ${ev1} in:" andReturn
          BetaReduction(ast, e -> ev1)
      case None =>
        ast
    }

  def elaborateGroupSheath(ast: Ast)(state: Option[String], replace: Ident, newIdent: Ident) = {
    val e  = replace
    val e1 = newIdent
    state match {
      case Some(v) =>
        val selector    = Property(e, "_2")
        val mappingBase = Property(e1, "_2")
        val mappingId   = Ident(newIdent.name, mappingBase.quat)
        val mapping     = Map(mappingBase, mappingId, Property(mappingId, v))
        trace"Elaborate Group Sheath. Replace ${selector} with ${mapping} in:" andReturn
          BetaReduction(ast, selector -> mapping)
      case None =>
        ast
    }
  }

  object NotGroupBy {
    def unapply(ast: Ast) =
      ast match {
        case p: GroupBy => None
        case _          => Some(ast)
      }
  }

  object MapClause {
    private trait MapClauseType
    private object MapClauseType {
      case object Map       extends MapClauseType
      case object ConcatMap extends MapClauseType
    }
    class Remaker private[MapClause] (tpe: MapClauseType) {
      def apply(a: Ast, b: Ident, c: Ast): Query =
        tpe match {
          case MapClauseType.Map       => Map(a, b, c)
          case MapClauseType.ConcatMap => ConcatMap(a, b, c)
        }
    }
    def unapply(ast: Ast) =
      ast match {
        case Map(a, b, c)       => Some((a, b, c, new Remaker(MapClauseType.Map)))
        case ConcatMap(a, b, c) => Some((a, b, c, new Remaker(MapClauseType.ConcatMap)))
        case _                  => None
      }
  }

  object UnionClause {
    private trait UnionClauseType
    private object UnionClauseType {
      case object Union    extends UnionClauseType
      case object UnionAll extends UnionClauseType
    }
    class Remaker private[UnionClause] (tpe: UnionClauseType) {
      def apply(a: Ast, b: Ast): Query =
        tpe match {
          case UnionClauseType.Union    => Union(a, b)
          case UnionClauseType.UnionAll => UnionAll(a, b)
        }
    }
    def unapply(ast: Ast) =
      ast match {
        case Union(a, b)    => Some((a, b, new Remaker(UnionClauseType.Union)))
        case UnionAll(a, b) => Some((a, b, new Remaker(UnionClauseType.UnionAll)))
        case _              => None
      }
  }

  override def apply(qq: Query)(implicit parent: History): (Query, StatefulTransformerWithStack[Option[String]]) = {
    implicit lazy val nextHistory = History.Child(qq, parent)
    lazy val prevType             = parent.ast.map(_.getClass.getSimpleName).getOrElse("Root")
    lazy val stateInfo            = s" [state:${state.toString},prev:${prevType.toString}] "
    lazy val parentShouldNeverHaveLeaves =
      parent.ast match {
        case Some(_: Aggregation) => false
        case Some(_: Distinct)    => false
        case Some(_: Query)       => true
        case _                    => false
      }

    qq match {
      // This is for cases of Agg(leaf) e.g. Agg(M(M(ent,e,e.v),e,e==123))
      // Not for cases of groupBy.map e.g. M(Grp(leaf,e,e),e,Agg(e))
      // Also if the top-level thing the Aggregation is a property e.g. Agg(M(ent,e,e.v))
      // we don't need to do anything (since that is what the SqlQuery.apply expects).
      case Aggregation(op, LeafQuat(ast)) =>
        val (ast1, s) = apply(ast)
        val ast2 =
          s.state match {
            case Some(prop) => Map(ast1, Ident("e", ast1.quat), Property(Ident("e", ast1.quat), prop))
            case None       => ast1
          }
        trace"Sheath Agg(query) with $stateInfo in $qq becomes" andReturn {
          (Aggregation(op, ast2), SheathLeafClauses(None, traceConfig))
        }

      /* GroupByMap is much easier to reason about that GroupBy(Map) in terms of sheathing. This why certain kinds of queries that
       * work with sheathing with GroupByMap will not work with groupBy. For example:
       * ```
       * case class Person(name: String, age: Int, lastModified: Long)
       *
       *  // Works Right!
       *  run { query[Person].groupByMap(p => p.name)(p => max(p.age)).filter(a => a > 1000) }
       *  // SELECT p.x FROM (SELECT MAX(p.age) AS x FROM Person p GROUP BY p.name) AS p WHERE p.x > 1000
       *
       *  // Works Right! (Although the query itself is not very useful!)
       *  run { query[Person].map(p => sql"someFunc(${p.age})".as[Int]).groupByMap(p => p)(p => max(p)).filter(a => a > 1000) }
       *  // SELECT p.x FROM (SELECT MAX(p._1) AS x FROM (SELECT someFunc(p.age) AS _1 FROM Person p) AS p GROUP BY p._1) AS p WHERE p.x > 1000
       *
       *  // Does not work!
       *  run { query[Person].groupBy(p => p.name).map { case (name, q) => q.map(_.age).max /*problematic clause*/ }.filter(a => a.getOrNull > 1000) }
       *  // SELECT p.* FROM (SELECT MAX(p.age) FROM Person p GROUP BY p.name) AS p WHERE p > 1000
       * ```
       * Sheathing the leaf-value p to a object like `class p(x:Int)` is difficult in the 3rd case because it is difficult to
       * ascertain how to re-write the `q.map(_.age).max` part in a way that can be transformed later (e.g. `Wrapper(x:q.map(_.age).max)`)
       * in a way that covers all the possible cases. This should theoretically be possible but requires more investigation.
       * The GroupByMap case however is much simpler. It's first-clause acts like a groupBy, it's second-clause acts like a Map.
       */
      case GroupByMap(query, eg, by, e, LeafQuat(body)) =>
        val innerState = query match {
          // If it's an infix inside e.g. Grp(i:Infix,..)(e,by) the higher-level apply should have changed it appropriately
          // by adding an extra Map step inside which has a CaseClass that holds a new attribute that we will pass around
          // e.g. from GrpTo(leaf,e,e)(e,Agg(e)) should have changed to GrpTo(M(leaf,e,CC(i->e)),e,e.i)(e,Agg(M(e->e.i)))
          case infix: io.getquill.ast.Infix =>
            val newId = Ident("i", infix.quat)
            Some((Map(infix, newId, CaseClass.Single("i" -> newId)), Some("i")))
          // If it's a query inside e.g. Grp(qry:Query,..)(e,by) the higher-level apply should have changed it appropriately
          // e.g. from GrpTo(ent,e,e.v)(e,Agg(e)) should have changed to GrpTo(ent,e,CC(v->e.v))(e,Agg(M(e->e.v))
          case _: Query =>
            val (q, s) = apply(query)
            Some((q, s.state))
          // Not sure if this is possible but in this case don't do anything
          case _ => None
        }
        innerState match {
          case Some((query1, s)) =>
            val eg1 = Ident(e.name, query1.quat)
            val by1 = elaborateSheath(by)(s, eg, eg1)
            // GrpTo signature is GrpTo(query,eg=>by)(e=>body) so both eg and e have the quat of query for example:
            //   GrpTo(query:Person,eg:Person,by:eg.name)(e:Person=>body)
            // so when query1 was originally a scalar:
            //   GrpTo(query:Int,eg:Int,by:eg)(e:Int=>body)
            // and changes to a value with a column:
            //   GrpTo(query:Value(i:Int),eg:Value(i:Int),by:eg)(e:Value(i:Int)=>body)
            // then eg and e also become this new value i.e. Value(i: Int)
            val e1          = Ident(e.name, query1.quat)
            val body1       = elaborateSheath(body)(s, e, e1)
            val (body2, s1) = sheathLeaf(body1)
            trace"Sheath GroupByMap(Grp,Agg) with $stateInfo in $qq becomes" andReturn {
              (GroupByMap(query1, eg1, by1, e1, body2), SheathLeafClauses(s1, traceConfig))
            }
          // If we ran into some kind of constructs inside the group-by don't do anything, just return the whole clause as-is
          case None =>
            trace"Could not understand Map(Grp,Agg) with $stateInfo clauses in $qq so returning same" andReturn {
              (qq, SheathLeafClauses(None, traceConfig))
            }
        }

      // This is the entry-point for all groupBy nodes which all must be followed by a .map clause
      // Typically the body of a groupBy.map is an aggregation.
      case Map(grpBy @ GroupBy(LeafQuat(query), eg, LeafQuat(by)), e, LeafQuat(body)) =>
        val innerState = query match {
          // If it's an infix inside e.g. Map(Grp(i:Infix),e,by) the higher-level apply should have changed it appropriately
          // by adding an extra Map step inside which has a CaseClass that holds a new attribute that we will pass around
          // e.g. from Map(Grp(leaf,e,e),e,Agg(e)) should have changed to Map(Grp(M(leaf,e,CC(i->e)),e,e.i),e,Agg(M(e->e.i)))
          case infix: io.getquill.ast.Infix =>
            val newId = Ident("i", infix.quat)
            Some((Map(infix, newId, CaseClass.Single("i" -> newId)), Some("i")))
          // If it's a query inside e.g. Map(Grp(qry:Query),e,by) the higher-level apply should have changed it appropriately
          // e.g. from Map(Grp(M(ent,e,e.v),e,e),e,Agg(e)) should have changed to Map(Grp(M(ent,e,CC(v->e.v)),e,e.v),e,Agg(M(e->e.v)))
          case _: Query =>
            val (q, s) = apply(query)
            Some((q, s.state))
          // Not sure if this is possible but in this case don't do anything
          case _ => None
        }
        innerState match {
          case Some((query1, s)) =>
            val eg1    = Ident(e.name, query1.quat)
            val by1    = elaborateSheath(by)(s, eg, eg1)
            val grpBy1 = GroupBy(query1, eg1, by1)
            val e1     = Ident(e.name, grpBy1.quat)
            val body1  = elaborateGroupSheath(body)(s, e, e1)
            // Typically this is an aggregation that we apply it to which goes Agg(e._2) to A(M(e._2,x,x.i)) or A(M(e._2,x,x.v))
            // the state returned from here is almost most cases should be None
            val (body2, s1) = SheathLeafClauses(None, traceConfig).apply(body1)
            trace"Sheath Map(Grp,Agg) with $stateInfo in $qq becomes" andReturn {
              (Map(grpBy1, e1, body2), s1)
            }
          // If we ran into some kind of constructs inside the group-by don't do anything, just return the whole clause as-is
          case None =>
            trace"Could not understand Map(Grp,Agg) with $stateInfo clauses in $qq so returning same" andReturn {
              (qq, SheathLeafClauses(None, traceConfig))
            }
        }

      // This clause happens for Map and ConcatMap, mostly for Map though!
      // This is the most important part of the SheathLeafClauses phase. It makes sure that any leaf-clauses selected
      // from a row are wrapped into Ast CaseClass nodes up to the top level.
      // Most typically M(M(ent,e,e.v),e,e) becomes M(ent,e,CC(v->e.v)),e,e) via the sheath-leaves stage and then M(ent,e,CC(v->e.v)),e,e.v)
      // via the elaborate-sheaths phase (the first elaborate-sheaths on the M(ent,e,e.v) clause should be None so that part is a no-op).
      // If it's M(M(M(ent,e,e.v),e,e),e,e) it will become M(M(M(ent,e,CC(v->e.v)),e,CC(v->e.v)),e,e.v)
      // This happens in two phases:
      // 1) Elaborate Sheaths - This means that if in the map.query we produced some kind of state "property" then
      //    we transform the alias `e` in the body to `e.property`.
      // 2) SheathLeaves - This means that if there are more nested stages that use a leaf projection (e.g. `e.v`)
      //    when we have to wrap that projection into another CaseClass which will provide the column information for us (e.g. `CC(v->e.v)`).
      //    Note that in certain phases that automatically expect a leaf-project e.g. aggregations we do not want to do this phase.
      //    that means that we need to track what the parent-ast (that contains this one) is.
      case MapClause(NotGroupBy(ent), e, LeafQuat(body), remake) =>
        val (ent1, s) = apply(ent)
        val e1        = Ident(e.name, ent1.quat)
        val bodyA     = elaborateSheath(body)(s.state, e, e1)
        val (bodyB, s1) =
          if (parentShouldNeverHaveLeaves)
            sheathLeaf(bodyA)
          else
            (bodyA, None)
        val (bodyC, _) = apply(bodyB)
        trace"Sheath Map(qry) with $stateInfo in $qq becomes" andReturn {
          (remake(ent1, e1, bodyC), SheathLeafClauses(s1, traceConfig))
        }

      case FlatMap(ent, e, body) =>
        val (ent1, s)   = apply(ent)
        val e1          = Ident(e.name, ent1.quat)
        val bodyA       = elaborateSheath(body)(s.state, e, e1)
        val (bodyB, s1) = s.apply(bodyA)
        trace"Sheath FlatMap(qry) with $stateInfo in $qq becomes" andReturn {
          (FlatMap(ent1, e1, bodyB), s1)
        }

      // When Sheathing leaves inside of a the two join clauses you get resulting state for them.
      // The issue is that it is very difficult to continue propagating the state onward so instead
      // we immediately map back to the leaf node before the join happens.
      // For example, say we have J(M(ent,e,e.v+123),M(ent,e,e.v+456)) that the sheathing changes to J(M(ent,e,CC(x->e.v+123)),M(ent,e,CC(x->e.v+456))).
      // We would then like to add an extra layer of mapping J(M(M(ent,e,CC(v->e.v+123)),e,e.x)),M(M(ent,e,CC(v->e.v+456)),e,e.x).
      // This might seem to do undo the original intent however, this if the parts e.g. v+123 are not actually reducible e.g. sql"AVG($v) OVER (PARTITION BY...)"
      // this additional information will not be removed via the applyMap normalization and actually help SqlQuery understand that the variable `x` should
      // be used to identify this column.
      // Also note that the alternative could be to produce an outer join e.g.
      // J(M(ent,e,CC(x->e.v+123)),M(ent,e,CC(x->e.v+456))) -> M(J(M(ent,e,CC(x->e.v+123)),M(ent,e,CC(x->e.v+456))),id,Tup(id._1.x,id._2.x))
      // Unfortunately however due to the ExpandJoin phase being non-well typed this would cause various kinds of queries to fail. Some
      // examples are in SqlQuerySpec. If ExpandJoin can be rewritten to be well-typed this approach can be re-examined.
      case Join(t, a, b, iA, iB, on) =>
        val (a1, sa)   = apply(a)
        val (b1, sb)   = apply(b)
        val (iA1, iB1) = (Ident(iA.name, a1.quat), Ident(iB.name, b1.quat))

        val a1m = sa.state.map(a => Map(a1, iA1, Property(iA1, a))).getOrElse(a1)
        val b1m = sb.state.map(a => Map(b1, iB1, Property(iB1, a))).getOrElse(b1)

        trace"Sheath Join with $stateInfo in $qq becomes" andReturn {
          (Join(t, a1m, b1m, iA, iB, on), SheathLeafClauses(None, traceConfig))
        }

      case Filter(ent, e, LeafQuat(body)) =>
        val (ent1, s) = apply(ent)
        val e1        = Ident(e.name, ent1.quat)
        // For filter clauses we want to go from: Filter(M(ent,e,e.v),e == 123) to Filter(M(ent,e,CC(v->e.v)),e,e.v == 123)
        // the body should not be re-sheathed since a body of CC(v->e.v == 123) would make no sense since
        // that's not even a boolean. Instead we just need to do e.v == 123.
        val bodyC = elaborateSheath(body)(s.state, e, e1)
        trace"Sheath Filter(qry) with $stateInfo in $qq becomes" andReturn {
          (Filter(ent1, e1, bodyC), s)
        }

      // This happens for Union and UnionAll
      // leaf value and right value calculations are independent
      // however if their states are not the same (and they are both defined)
      // we need to climb back into the CaseClasses that both created and change the property that they map
      // to to be the same
      case UnionClause(LeafQuat(left), LeafQuat(right), remake) =>
        val (left1, sl)  = apply(left)
        val (right1, sr) = apply(right)
        val (left2, right2, s) =
          (sl.state, sr.state) match {
            // If they both have the same state content e.g. U(M(ent,e,CC(v->e.v)),M(ent,e,CC(v->e.v)))
            case (Some(l), Some(r)) if (l == r) =>
              (left1, right1, sr.state)
            // If they have a different state e.g. U(M(ent,e.CC(a->e.a)),M(ent,e,CC(b->e.b)))
            case (Some(l), Some(r)) if (l != r) =>
              // Change the property inside i.e. go from:
              //   U(M(ent,e.CC(a->e.a)),M(ent,e,CC(b->e.b)))
              // to:
              //   U( M{M(ent,e.CC(a->e.a)),CC(u->e.a)}, M{M(ent,e,CC(b->e.b)),CC(u->e.b)} )
              // should beta reduce to?
              //   U(M(ent,e.CC(`u`->e.a)),M(ent,e,CC(`u`->e.b)))
              val el = Ident("e", left1.quat)
              val pl = Property(el, l)
              val er = Ident("e", right1.quat)
              val pr = Property(er, r)
              (Map(left1, el, CaseClass.Single("u" -> pl)), Map(right1, er, CaseClass.Single("u" -> pr)), Some("u"))
            case (None, None) =>
              (left1, right1, None)
            // if only one side has a state etc... do not do anything, technically
            // this should be a warning
            case _ =>
              (left1, right1, None)
          }
        trace"Sheath Union(qry) with $stateInfo in $qq becomes" andReturn {
          (remake(left2, right2), SheathLeafClauses(s, traceConfig))
        }

      case _ => super.apply(qq)
    }
  }

}

private[getquill] class SheathLeafClausesApply(traceConfig: TraceConfig) {
  def apply(q: Ast) = new SheathLeafClauses(None, traceConfig).apply(q)(History.Root)._1
}
