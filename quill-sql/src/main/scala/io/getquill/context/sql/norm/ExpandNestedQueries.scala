package io.getquill.context.sql.norm

import io.getquill.ast.Ast
import io.getquill.ast.Ident
import io.getquill.ast.Property
import io.getquill.ast.StatefulTransformer
import io.getquill.context.sql.FlattenSqlQuery
import io.getquill.context.sql.FromContext
import io.getquill.context.sql.InfixContext
import io.getquill.context.sql.JoinContext
import io.getquill.context.sql.QueryContext
import io.getquill.context.sql.SelectValue
import io.getquill.context.sql.SetOperationSqlQuery
import io.getquill.context.sql.SqlQuery
import io.getquill.context.sql.TableContext
import io.getquill.context.sql.UnaryOperationSqlQuery
import io.getquill.context.sql.FlatJoinContext

object ExpandNestedQueries {

  def apply(q: SqlQuery, references: collection.Set[Property]): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        expandNested(q.copy(select = expandSelect(q.select, references)))
      case SetOperationSqlQuery(a, op, b) =>
        SetOperationSqlQuery(apply(a, references), op, apply(b, references))
      case UnaryOperationSqlQuery(op, q) =>
        UnaryOperationSqlQuery(op, apply(q, references))
    }

  private def expandNested(q: FlattenSqlQuery): SqlQuery =
    q match {
      case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select, distinct) =>
        val asts = Nil ++ where ++ groupBy ++ orderBy.map(_.ast) ++ limit ++ offset ++ select.map(_.ast)
        val from = q.from.map(expandContext(_, asts))
        q.copy(from = from)
    }

  private def expandContext(s: FromContext, asts: List[Ast]): FromContext =
    s match {
      case QueryContext(q, alias) =>
        QueryContext(apply(q, references(alias, asts)), alias)
      case JoinContext(t, a, b, on) =>
        JoinContext(t, expandContext(a, asts :+ on), expandContext(b, asts :+ on), on)
      case FlatJoinContext(t, a, on) =>
        FlatJoinContext(t, expandContext(a, asts :+ on), on)
      case _: TableContext | _: InfixContext => s
    }

  private def expandSelect(select: List[SelectValue], references: collection.Set[Property]) =
    references.toList match {
      case Nil => select
      case refs =>
        refs.map {
          case Property(Property(_, tupleElem), prop) =>
            val p = Property(select(tupleElem.drop(1).toInt - 1).ast, prop)
            SelectValue(p, Some(prop))
          case Property(_, tupleElem) if (tupleElem.matches("_[0-9]*")) =>
            SelectValue(select(tupleElem.drop(1).toInt - 1).ast, Some(tupleElem))
          case Property(_, name) =>
            select match {
              case List(SelectValue(i: Ident, _)) =>
                SelectValue(Property(i, name))
              case other =>
                SelectValue(Ident(name))
            }
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
