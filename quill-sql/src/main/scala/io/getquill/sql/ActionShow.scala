package io.getquill.sql

import io.getquill.util.Show._
import io.getquill.ast._

object ActionShow {

  import ExprShow.exprShow

  implicit val actionShow: Show[Action] = new Show[Action] {
    def show(a: Action) =
      a match {
        case Insert(Table(table), assignments) =>
          val columns = assignments.map(_.property: Expr)
          val values = assignments.map(_.value)
          s"INSERT INTO $table (${columns.show}) VALUES (${values.show})"
        case Update(Table(table), assignments) =>
          s"UPDATE $table SET ${set(assignments)}"
        case Update(Filter(Table(table), x, where), assignments) =>
          s"UPDATE $table SET ${set(assignments)} WHERE ${where.show}"
        case Delete(Filter(Table(table), x, where)) =>
          s"DELETE FROM $table WHERE ${where.show}"
        case Delete(Table(table)) =>
          s"DELETE FROM $table"
        case other =>
          throw new IllegalStateException(s"Invalid action '$a'")
      }
  }

  private def set(assignments: List[Assignment]) =
    assignments.map(a => s"${a.property.name} = ${a.value.show}").mkString(", ")

  implicit def refShow: Show[Ref] = new Show[Ref] {
    def show(e: Ref) =
      e match {
        case Property(_, name) => name
        case other =>
          import ExprShow.valueShow
          import ExprShow.identShow
          ExprShow.refShow.show(other)
      }
  }

}