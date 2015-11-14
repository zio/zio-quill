package io.getquill.source.sql

import io.getquill.ast._

object ExpandNestedQueries {

  def apply(q: SqlQuery, references: Set[Property]): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        expandNested(q.copy(select = expandSelect(q.select, references)))
      case SetOperationSqlQuery(a, op, b) =>
        SetOperationSqlQuery(apply(a, references), op, apply(b, references))
    }

  private def expandNested(q: FlattenSqlQuery): SqlQuery =
    q match {
      case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select) =>
        val asts = Nil ++ where ++ groupBy ++ limit ++ offset ++ select.map(_.ast)
        val from = q.from.map(expandSource(_, asts))
        q.copy(from = from)
    }

  private def expandSource(s: Source, asts: List[Ast]): Source =
    s match {
      case QuerySource(q, alias) =>
        QuerySource(apply(q, references(alias, asts)), alias)
      case OuterJoinSource(t, a, b, on) =>
        OuterJoinSource(t, expandSource(a, asts :+ on), expandSource(b, asts :+ on), on)
      case _: TableSource | _: InfixSource => s
    }

  private def expandSelect(select: List[SelectValue], references: Set[Property]) =
    references.toList match {
      case Nil => select
      case refs =>
        refs.map {
          case Property(Property(_, tupleElem), prop) =>
            val p = Property(select(tupleElem.drop(1).toInt - 1).ast, prop)
            SelectValue(p, Some(tupleElem + prop))
          case Property(_, tupleElem) if (tupleElem.matches("_[0-9]*")) =>
            SelectValue(select(tupleElem.drop(1).toInt - 1).ast, Some(tupleElem))
          case Property(_, name) =>
            SelectValue(Ident(name))
        }
    }

  private def references(alias: String, asts: List[Ast]) =
    References(State(Ident(alias), Nil))(asts)(_.apply)._2.state.references.toSet
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
