package io.getquill.sql.norm.migration

import io.getquill.NamingStrategy
import io.getquill.ast.Ast
import io.getquill.ast.Ident
import io.getquill.ast._
import io.getquill.ast.Visibility.Visible
import io.getquill.context.sql._

import scala.collection.mutable.LinkedHashSet
import io.getquill.util.Interpolator
import io.getquill.util.Messages.TraceType.NestedQueryExpansion

import io.getquill.norm.{ BetaReduction, TypeBehavior }
import io.getquill.quat.Quat
import io.getquill.sql.norm._

import scala.collection.mutable

class ExpandNestedQueriesLegacy(strategy: NamingStrategy) {

  val interp = new Interpolator(NestedQueryExpansion, 3)
  import interp._

  def apply(q: SqlQuery, references: List[Property]): SqlQuery =
    apply(q, LinkedHashSet.empty ++ references, true)

  // Using LinkedHashSet despite the fact that it is mutable because it has better characteristics then ListSet.
  // Also this collection is strictly internal to ExpandNestedQueries and exposed anywhere else.
  private def apply(q: SqlQuery, references: LinkedHashSet[Property], isTopLevel: Boolean = false): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        val expand = expandNested(q.copy(select = ExpandSelectLegacy(q.select, references, strategy))(q.quat), isTopLevel)
        trace"Expanded Nested Query $q into $expand".andLog()
        expand
      case SetOperationSqlQuery(a, op, b) =>
        SetOperationSqlQuery(apply(a, references), op, apply(b, references))(q.quat)
      case UnaryOperationSqlQuery(op, q) =>
        UnaryOperationSqlQuery(op, apply(q, references))(q.quat)
    }

  private def expandNested(q: FlattenSqlQuery, isTopLevel: Boolean): SqlQuery =
    q match {
      case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select, distinct) =>
        val asts = Nil ++ select.map(_.ast) ++ where ++ groupBy ++ orderBy.map(_.ast) ++ limit ++ offset
        val expansions = q.from.map(expandContext(_, asts))
        val from = expansions.map(_._1)
        val references = expansions.flatMap(_._2)

        val replacedRefs = references.map(ref => (ref, unhideAst(ref)))

        // Need to unhide properties that were used during the query
        def replaceProps(ast: Ast) =
          BetaReduction(ast, TypeBehavior.ReplaceWithReduction, replacedRefs: _*) // Since properties could not actually be inside, don't typecheck the reduction
        def replacePropsOption(ast: Option[Ast]) =
          ast.map(replaceProps(_))

        def distinctIfNotTopLevel(values: List[SelectValue]) =
          if (isTopLevel)
            values
          else
            values.distinct

        /*
         * In sub-queries, need to make sure that the same field/alias pair is not selected twice
         * which is possible when aliases are used. For example, something like this:
         *
         * case class Emb(id: Int, name: String) extends Embedded
         * case class Parent(id: Int, name: String, emb: Emb) extends Embedded
         * case class GrandParent(id: Int, par: Parent)
         * val q = quote { query[GrandParent].map(g => g.par).distinct.map(p => (p.name, p.emb, p.id, p.emb.id)).distinct.map(tup => (tup._1, tup._2, tup._3, tup._4)).distinct }
         * Will cause double-select inside the innermost subselect:
         * SELECT DISTINCT theParentName AS theParentName, id AS embid, theName AS embtheName, id AS id, id AS embid FROM GrandParent g
         * Note how embid occurs twice? That's because (p.emb.id, p.emb) are expanded into (p.emb.id, p.emb.id, e.emb.name).
         *
         * On the other hand if the query is top level we need to make sure not to do this deduping or else the encoders won't work since they rely on clause positions
         * For example, something like this:
         * val q = quote { query[GrandParent].map(g => g.par).distinct.map(p => (p.name, p.emb, p.id, p.emb.id)) }
         * Would normally expand to this:
         * SELECT p.theParentName, p.embid, p.embtheName, p.id, p.embid FROM ...
         * Note now embed occurs twice? We need to maintain this because the second element of the output tuple
         * (p.name, p.emb, p.id, p.emb.id) needs the fields p.embid, p.embtheName in that precise order in the selection
         * or they cannot be encoded.
         */
        val newSelects =
          distinctIfNotTopLevel(select.map(sv => sv.copy(ast = replaceProps(sv.ast))))

        q.copy(
          select = newSelects,
          from = from,
          where = replacePropsOption(where),
          groupBy = replacePropsOption(groupBy),
          orderBy = orderBy.map(ob => ob.copy(ast = replaceProps(ob.ast))),
          limit = replacePropsOption(limit),
          offset = replacePropsOption(offset)
        )(q.quat)

    }

  def unhideAst(ast: Ast): Ast =
    Transform(ast) {
      case Property.Opinionated(a, n, r, v) =>
        Property.Opinionated(unhideAst(a), n, r, Visible)
    }

  private def unhideProperties(sv: SelectValue) =
    sv.copy(ast = unhideAst(sv.ast))

  private def expandContext(s: FromContext, asts: List[Ast]): (FromContext, LinkedHashSet[Property]) =
    s match {
      case QueryContext(q, alias) =>
        val refs = references(alias, asts)
        (QueryContext(apply(q, refs), alias), refs)
      case JoinContext(t, a, b, on) =>
        val (left, leftRefs) = expandContext(a, asts :+ on)
        val (right, rightRefs) = expandContext(b, asts :+ on)
        (JoinContext(t, left, right, on), leftRefs ++ rightRefs)
      case FlatJoinContext(t, a, on) =>
        val (next, refs) = expandContext(a, asts :+ on)
        (FlatJoinContext(t, next, on), refs)
      case _: TableContext | _: InfixContext => (s, new mutable.LinkedHashSet[Property]())
    }

  private def references(alias: String, asts: List[Ast]) =
    LinkedHashSet.empty ++ (References(State(Ident(alias, Quat.Value), Nil))(asts)(_.apply)._2.state.references) // TODO scrap this whole thing with quats
}
