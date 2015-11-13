package io.getquill.source.sql.idiom

import io.getquill.ast._
import io.getquill.util.Show._
import io.getquill.source.sql.naming.NamingStrategy

trait MySQLDialect
    extends SqlIdiom
    with OffsetWithoutLimitWorkaround {

  override def prepare(sql: String) =
    s"PREPARE p${sql.hashCode.abs} FROM '$sql'"

  override implicit def operationShow(implicit propertyShow: Show[Property], strategy: NamingStrategy): Show[Operation] =
    Show[Operation] {
      case BinaryOperation(a, StringOperator.`+`, b) => s"CONCAT(${a.show}, ${b.show})"
      case other                                     => super.operationShow.show(other)
    }
}
object MySQLDialect extends MySQLDialect