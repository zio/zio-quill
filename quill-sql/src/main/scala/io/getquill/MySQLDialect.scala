package io.getquill

import io.getquill.ast.Asc
import io.getquill.ast.AscNullsFirst
import io.getquill.ast.AscNullsLast
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Desc
import io.getquill.ast.DescNullsFirst
import io.getquill.ast.DescNullsLast
import io.getquill.ast.Operation
import io.getquill.ast.Property
import io.getquill.ast.StringOperator
import io.getquill.context.sql.OrderByCriteria
import io.getquill.util.Show.Show
import io.getquill.util.Show.Shower
import io.getquill.context.sql.idiom.OffsetWithoutLimitWorkaround
import io.getquill.context.sql.idiom.SqlIdiom

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
