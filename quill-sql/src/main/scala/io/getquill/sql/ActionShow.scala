package io.getquill.sql

import ExprShow.exprShow
import ExprShow.identShow
import ExprShow.valueShow
import io.getquill.ast.Action
import io.getquill.ast.Assignment
import io.getquill.ast.Delete
import io.getquill.ast.Expr
import io.getquill.ast.Filter
import io.getquill.ast.Insert
import io.getquill.ast.Property
import io.getquill.ast.Ref
import io.getquill.ast.Table
import io.getquill.ast.Update
import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.util.Show.listShow

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