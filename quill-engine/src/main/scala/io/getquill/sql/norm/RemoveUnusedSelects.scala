package io.getquill.sql.norm

import io.getquill.ast.{Ast, CollectAst, Ident, Property, StatefulTransformer}
import io.getquill.context.sql.{
  DistinctKind,
  FlatJoinContext,
  FlattenSqlQuery,
  FromContext,
  InfixContext,
  JoinContext,
  QueryContext,
  SelectValue,
  SetOperationSqlQuery,
  SqlQuery,
  TableContext,
  UnaryOperationSqlQuery
}
import io.getquill.norm.PropertyMatryoshka
import io.getquill.quat.Quat

import scala.collection.mutable
import scala.collection.mutable.LinkedHashSet

/**
 * Filter out unused subquery properties. This is safe to do after
 * `ExpandNestedQueries` now since all properties have been propagated from
 * quats from parent to child SQL select trees.
 */
object RemoveUnusedSelects {

  def apply(q: SqlQuery): SqlQuery =
    apply(q, LinkedHashSet.empty, true)

  private def apply(q: SqlQuery, references: LinkedHashSet[Property], isTopLevel: Boolean = false): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        // Filter the select values based on which references are used in the upper-level query
        val newSelect = filterUnused(q.select, references.toSet)

        // If the select is not a distinct (since if it is distinct the result will be different if more fields are selected),
        // same with groupBy, sortBy, and limit (some obscure properties is databases may have different results if different columns are selected with a limit)
        // it is not top level (all top level queries will have no references outer-selected from them)
        // since they are top level the filter the selects.
        // Also, if nothing is being selected (specifically including a check on references.size to be explicit about it but technically it is covered by other cases),
        // do not filter the selects either since the
        // alternative is to have a "select *" from having a concrete set of selects is almost always better then a "select *"
        val doSelectFiltration =
          !isTopLevel && !q.distinct.isDistinct && q.groupBy.isEmpty && q.orderBy.isEmpty && q.limit.isEmpty &&
            references.nonEmpty && newSelect.nonEmpty

        // Gather asts used here - i.e. push every property we are using to the From-Clause selects to make
        // sure that they in turn are using them.
        // Since we first need to replace select values from super queries onto sub queries,
        // take the newly filtered selects instead of the ones in the query which are pre-filtered
        // ... unless we are on the top level query. Since in the top level query 'references'
        // will always be empty we need to copy through the entire select clause
        val asts = gatherAsts(q, if (doSelectFiltration) newSelect else q.select)

        // recurse into the from clause with ExpandContext
        val fromContextsAndSuperProps = q.from.map(expandContext(_, asts))
        val fromContexts              = fromContextsAndSuperProps.map(_._1)

        if (doSelectFiltration) {
          // copy the new from clause and filter aliases
          q.copy(from = fromContexts, select = newSelect)(q.quat)
        } else {
          // If we are on the top level, the list of aliases being used by clauses outer to 'us'
          // don't exist since we are the outermost level of the sql. Therefore no filtration
          // should happen in that case.
          q.copy(from = fromContexts)(q.quat)
        }

      case SetOperationSqlQuery(a, op, b) =>
        SetOperationSqlQuery(apply(a, references), op, apply(b, references))(q.quat)
      case UnaryOperationSqlQuery(op, q) =>
        UnaryOperationSqlQuery(op, apply(q, references))(q.quat)
    }

  private def filterUnused(select: List[SelectValue], references: Set[Property]): List[SelectValue] = {
    val usedAliases = references.map { case PropertyMatryoshka(_, list, _) =>
      list.mkString
    }.toSet
    select.filter(sv =>
      sv.alias.forall(aliasValue => usedAliases.contains(aliasValue)) ||
        hasImpureInfix(sv.ast)
    )
  }

  private def hasImpureInfix(ast: Ast) =
    CollectAst.byType[io.getquill.ast.Infix](ast).find(i => !i.pure).isDefined

  private def gatherAsts(q: FlattenSqlQuery, newSelect: List[SelectValue]): List[Ast] =
    q match {
      case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select, distinct) =>
        Nil ++ newSelect.map(_.ast) ++ where ++ groupBy ++ orderBy.map(_.ast) ++ limit ++ offset
    }

  private def expandContext(s: FromContext, asts: List[Ast]): (FromContext, LinkedHashSet[Property]) =
    s match {
      case QueryContext(q, alias) =>
        val refs      = references(alias, asts)
        val reapplied = apply(q, refs)
        (QueryContext(reapplied, alias), refs)
      case JoinContext(t, a, b, on) =>
        val (left, leftRefs)   = expandContext(a, asts :+ on)
        val (right, rightRefs) = expandContext(b, asts :+ on)
        (JoinContext(t, left, right, on), leftRefs ++ rightRefs)
      case FlatJoinContext(t, a, on) =>
        val (next, refs) = expandContext(a, asts :+ on)
        (FlatJoinContext(t, next, on), refs)
      case _: TableContext | _: InfixContext => (s, new mutable.LinkedHashSet[Property]())
    }

  private def references(alias: String, asts: List[Ast]) =
    LinkedHashSet.empty ++ (References(State(Ident(alias, Quat.Value), Nil))(asts)(_.apply)._2.state.references)
}

case class State(ident: Ident, references: List[Property])

case class References(val state: State) extends StatefulTransformer[State] {

  import state._

  override def apply(a: Ast) =
    a match {
      case `reference`(p) => (p, References(State(ident, references :+ p)))
      case other          => super.apply(a)
    }

  object reference {
    def unapply(p: Property): Option[Property] =
      p match {
        case Property(`ident`, name)      => Some(p)
        case Property(reference(_), name) => Some(p)
        case other                        => None
      }
  }
}
