package io.getquill.source.sql.idiom

import io.getquill.util.Show._
import io.getquill.source.sql.OrderByCriteria
import io.getquill.source.sql.OrderByCriteria

trait NullsOrderingClause {
  this: SqlIdiom =>

  override protected def showOrderBy(criterias: List[OrderByCriteria]) =
    s" ORDER BY ${criterias.map(showCriteria).mkString(", ")}"

  private def showCriteria(criteria: OrderByCriteria) =
    criteria.desc match {
      case false => s"${criteria.show} NULLS FIRST"
      case true  => s"${criteria.show} NULLS LAST"
    }
}
