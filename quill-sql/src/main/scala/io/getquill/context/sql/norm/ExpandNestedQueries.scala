package io.getquill.context.sql.norm

import io.getquill.NamingStrategy
import io.getquill.ast.Ast
import io.getquill.ast.Ident
import io.getquill.ast._
import io.getquill.ast.StatefulTransformer
import io.getquill.ast.Visibility.Visible
import io.getquill.context.sql._

import scala.collection.mutable.LinkedHashSet
import io.getquill.util.Interpolator
import io.getquill.util.Messages.TraceType.NestedQueryExpansion
import io.getquill.context.sql.norm.nested.ExpandSelect

import scala.collection.mutable

class ExpandNestedQueries(strategy: NamingStrategy) {

  val interp = new Interpolator(NestedQueryExpansion, 3)
  import interp._

  def apply(q: SqlQuery, references: List[Property]): SqlQuery =
    apply(q, LinkedHashSet.empty ++ references)

  // Using LinkedHashSet despite the fact that it is mutable because it has better characteristics then ListSet.
  // Also this collection is strictly internal to ExpandNestedQueries and exposed anywhere else.
  private def apply(q: SqlQuery, references: LinkedHashSet[Property]): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        val expand = expandNested(q.copy(select = ExpandSelect(q.select, references, strategy)))
        trace"Expanded Nested Query $q into $expand" andLog ()
        expand
      case SetOperationSqlQuery(a, op, b) =>
        SetOperationSqlQuery(apply(a, references), op, apply(b, references))
      case UnaryOperationSqlQuery(op, q) =>
        UnaryOperationSqlQuery(op, apply(q, references))
    }

  private def expandNested(q: FlattenSqlQuery): SqlQuery =
    q match {
      case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select, distinct) =>
        val asts = Nil ++ select.map(_.ast) ++ where ++ groupBy ++ orderBy.map(_.ast) ++ limit ++ offset
        val expansions = q.from.map(expandContext(_, asts))
        val from = expansions.map(_._1)
        val references = expansions.flatMap(_._2)
        val modifiedSelects =
          select
            .map(s =>
              if (references.contains(s.ast))
                trace"Un-hide Select $s:" andReturn unhideProperties(s)
              else s)

        q.copy(select = modifiedSelects, from = from)
    }

  private def unhideProperties(sv: SelectValue) = {
    def unhideRecurse(ast: Ast): Ast =
      Transform(ast) {
        case Property.Opinionated(a, n, r, v) =>
          Property.Opinionated(unhideRecurse(a), n, r, Visible)
      }
    sv.copy(ast = unhideRecurse(sv.ast))
  }

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
    LinkedHashSet.empty ++ (References(State(Ident(alias), Nil))(asts)(_.apply)._2.state.references)
}

case class State(ident: Ident, references: List[Property])

case class References(val state: State)
  extends StatefulTransformer[State] {

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
