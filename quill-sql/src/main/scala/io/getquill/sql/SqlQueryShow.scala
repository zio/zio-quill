package io.getquill.sql

import ExprShow.exprShow
import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.util.Show.listShow

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

  implicit val sourceShow: Show[Source] = new Show[Source] {
    def show(source: Source) =
      s"${source.table} ${source.alias}"
  }
}
