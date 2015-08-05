package io.getquill.sql

import ExprShow.exprShow
import io.getquill.util.Show._
import io.getquill.ast.Ref
import io.getquill.ast.Property

object SqlDeleteShow {

  import ExprShow.exprShow
  import ExprShow.valueShow
  import ExprShow.identShow

  implicit val sqlDeleteShow: Show[SqlQuery] = new Show[SqlQuery] {
    def show(e: SqlQuery) =
      (e.from, e.where) match {
        case (List(source), None) =>
          s"DELETE FROM ${source.show}"
        case (List(source), Some(where)) =>
          import ExprShow.exprShow
          s"DELETE FROM ${source.show} WHERE ${where.show}"
        case other =>
          throw new IllegalStateException("Update statement must use only one table.")
      }
  }

  implicit def refShow: Show[Ref] = new Show[Ref] {
    def show(e: Ref) =
      e match {
        case Property(ref, name) => name
        case other =>
          ExprShow.refShow.show(other)
      }
  }

  implicit val sourceShow: Show[Source] = new Show[Source] {
    def show(source: Source) =
      s"${source.table}"
  }
}
