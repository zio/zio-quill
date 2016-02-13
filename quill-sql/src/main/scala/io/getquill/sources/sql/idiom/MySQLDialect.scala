package io.getquill.sources.sql.idiom

import io.getquill.ast._
import io.getquill.naming.NamingStrategy
import io.getquill.util.Show._
import io.getquill.sources.sql.OrderByCriteria

trait MySQLDialect
    extends SqlIdiom
    with OffsetWithoutLimitWorkaround {

  override def prepare(sql: String) =
    s"PREPARE p${sql.hashCode.abs} FROM '${sql.replace("'", "\\'")}'"

  override implicit def operationShow(implicit propertyShow: Show[Property], strategy: NamingStrategy): Show[Operation] =
    Show[Operation] {
      case BinaryOperation(a, StringOperator.`+`, b) => s"CONCAT(${a.show}, ${b.show})"
      case other                                     => super.operationShow.show(other)
    }

  override implicit def orderByCriteriaShow(implicit strategy: NamingStrategy): Show[OrderByCriteria] = Show[OrderByCriteria] {
    case OrderByCriteria(prop, AscNullsFirst | Asc)  => s"${prop.show} ASC"
    case OrderByCriteria(prop, DescNullsFirst)       => s"ISNULL(${prop.show}) DESC, ${prop.show} DESC"
    case OrderByCriteria(prop, AscNullsLast)         => s"ISNULL(${prop.show}) ASC, ${prop.show} ASC"
    case OrderByCriteria(prop, DescNullsLast | Desc) => s"${prop.show} DESC"
  }
}

object MySQLDialect extends MySQLDialect
