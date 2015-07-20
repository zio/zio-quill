package io.getquill.sql

import ExprShow.exprShow
import ExprShow.predicateShow
import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower

object SqlQueryShow {

  import ExprShow._

  implicit val sqlQueryShow: Show[SqlQuery] = new Show[SqlQuery] {
    def show(e: SqlQuery) = {
      s"""
SELECT ${e.select.show}
  FROM ${e.from.show}
 WHERE ${e.where.show}
"""
    }
  }

  implicit val sourceListShow: Show[List[Source]] = new Show[List[Source]] {
    def show(list: List[Source]) =
      list.map(e => s"${e.table} ${e.alias}").mkString(",\n       ")
  }
}
