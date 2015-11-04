package io.getquill.source.sql.idiom

import io.getquill.ast._
import io.getquill.util.Show._
import io.getquill.source.sql.naming.NamingStrategy

object MySQLDialect
    extends SqlIdiom
    with OffsetWithoutLimitWorkaround {

  override def prepare(sql: String) =
    Some(s"PREPARE p${sql.hashCode.abs} FROM '$sql'")

  override implicit def operationShow(implicit propertyShow: Show[Property], strategy: NamingStrategy): Show[Operation] = new Show[Operation] {
    def show(e: Operation) =
      e match {
        case BinaryOperation(a, StringOperator.`+`, b) => s"CONCAT(${a.show}, ${b.show})"
        case other                                     => MySQLDialect.super.operationShow.show(other)
      }
  }
}
