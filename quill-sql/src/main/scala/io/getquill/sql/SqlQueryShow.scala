package io.getquill.sql

import ExprShow.exprShow
import io.getquill.util.Show._

object SqlQueryShow {

  import ExprShow._

  implicit val sqlQueryShow: Show[SqlQuery] = new Show[SqlQuery] {
    def show(e: SqlQuery) =
      e.where match {
        case None =>
          s"SELECT ${e.select.show} FROM ${e.from.show}"
        case Some(where) =>
          s"SELECT ${e.select.show} FROM ${e.from.show} WHERE ${where.show}"
      }
  }

  implicit val sourceListShow: Show[List[Source]] = new Show[List[Source]] {
    def show(list: List[Source]) =
      list.map(e => s"${e.table} ${e.alias}").mkString(", ")
  }
}
