package io.getquill.sql

import ExprShow.exprShow
import io.getquill.util.Show._
import io.getquill.ast.Tuple
import io.getquill.ast.Expr
import io.getquill.ast.Property

object SqlInsertShow {

  import ExprShow._

  implicit val sqlInsertShow: Show[SqlQuery] = new Show[SqlQuery] {
    def show(q: SqlQuery) =
      q.where match {
        case None =>
          s"INSERT INTO ${table(q)} (${columns(q)}) VALUES (${parameters(q)})"
        case Some(where) =>
          fail(s"Insert query can't have a filter condition ($where)")
      }
  }

  private def parameters(q: SqlQuery) =
    q.select match {
      case Tuple(values) => List.fill(values.size)("?").mkString(", ")
      case other         => throw new IllegalStateException(s"Insert query can't use multiple tables.")
    }

  private def columns(q: SqlQuery) =
    q.select match {
      case Tuple(values) => values.map(column).mkString(", ")
      case other         => throw new IllegalStateException(s"Insert query can't use multiple tables.")
    }

  private def column(e: Expr) =
    e match {
      case Property(_, name) => name
      case other             => fail(s"Not a valid insert column: $other")
    }

  private def table(q: SqlQuery) =
    q.from match {
      case List(source) => source.table
      case other        => throw new IllegalStateException(s"Insert query can't use multiple tables.")
    }

  implicit val sourceShow: Show[Source] = new Show[Source] {
    def show(source: Source) =
      source.table
  }

  private def fail(message: String) =
    throw new IllegalArgumentException(message)
}
