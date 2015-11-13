package io.getquill.source.sql.idiom

import io.getquill.naming.NamingStrategy
import io.getquill.source.sql.OrderByCriteria
import io.getquill.util.Show.Shower

trait NullsOrderingClause {
  this: SqlIdiom =>

  override protected def showOrderBy(criterias: List[OrderByCriteria])(implicit strategy: NamingStrategy) =
    s" ORDER BY ${criterias.map(showCriteria).mkString(", ")}"

  private def showCriteria(criteria: OrderByCriteria)(implicit strategy: NamingStrategy) =
    criteria.desc match {
      case false => s"${criteria.show} NULLS FIRST"
      case true  => s"${criteria.show} NULLS LAST"
    }
}
