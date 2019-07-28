package io.getquill.context.sql.norm

import io.getquill.ast.Ast
import io.getquill.ast.Ident
import io.getquill.ast._
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
import scala.collection.mutable.LinkedHashSet

object ExpandNestedQueries {

  def apply(q: SqlQuery, references: List[Property]): SqlQuery =
    apply(q, LinkedHashSet.empty ++ references)

  // Using LinkedHashSet despite the fact that it is mutable because it has better characteristics then ListSet.
  // Also this collection is strictly internal to ExpandNestedQueries and exposed anywhere else.
  private def apply(q: SqlQuery, references: LinkedHashSet[Property]): SqlQuery =
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
        val asts = Nil ++ select.map(_.ast) ++ where ++ groupBy ++ orderBy.map(_.ast) ++ limit ++ offset
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

  private def expandSelect(select: List[SelectValue], references: LinkedHashSet[Property]) = {

    object TupleIndex {
      def unapply(s: String): Option[Int] =
        if (s.matches("_[0-9]*"))
          Some(s.drop(1).toInt - 1)
        else
          None
    }

    def expandReference(ref: Property): SelectValue = {

      def concat(alias: Option[String], idx: Int) =
        Some(s"${alias.getOrElse("")}_${idx + 1}")

      ref match {
        case Property(ast: Property, TupleIndex(idx)) =>
          expandReference(ast) match {
            case SelectValue(Tuple(elems), alias, c) =>
              SelectValue(elems(idx), concat(alias, idx), c)
            case SelectValue(ast, alias, c) =>
              SelectValue(ast, concat(alias, idx), c)
          }
        case Property(ast: Property, name) =>
          expandReference(ast) match {
            case SelectValue(ast, nested, c) =>
              SelectValue(Property(ast, name), Some(s"${nested.getOrElse("")}$name"), c)
          }
        case Property(_, TupleIndex(idx)) =>
          select(idx) match {
            case SelectValue(ast, alias, c) =>
              SelectValue(ast, concat(alias, idx), c)
          }
        case Property(_, name) =>
          select match {
            case List(SelectValue(cc: CaseClass, alias, c)) =>
              SelectValue(cc.values.toMap.apply(name), Some(name), c)
            case List(SelectValue(i: Ident, _, c)) =>
              SelectValue(Property(i, name), None, c)
            case other =>
              SelectValue(Ident(name), Some(name), false)
          }
      }
    }

    references.toList match {
      case Nil  => select
      case refs => refs.map(expandReference)
    }
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
