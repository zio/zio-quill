package io.getquill.sql

import AstShow.astShow
import AstShow.identShow
import AstShow.valueShow
import io.getquill.ast.Action
import io.getquill.ast.Assignment
import io.getquill.ast.Delete
import io.getquill.ast.Ast
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

  import AstShow.astShow

  implicit val actionShow: Show[Action] = new Show[Action] {
    def show(a: Action) =
      a match {

        case Insert(Table(table), assignments) =>
          val columns = assignments.map(_.property: Ast)
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
          import AstShow.valueShow
          import AstShow.identShow
          AstShow.refShow.show(other)
      }
  }

}