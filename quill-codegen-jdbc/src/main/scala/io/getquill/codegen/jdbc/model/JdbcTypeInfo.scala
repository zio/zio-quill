package io.getquill.codegen.jdbc.model

import io.getquill.codegen.model.JdbcColumnMeta

case class JdbcTypeInfo(jdbcType: Int, size: Int, typeName: Option[String])
object JdbcTypeInfo {
  def apply(cs: JdbcColumnMeta): JdbcTypeInfo = JdbcTypeInfo(cs.dataType, cs.size, Some(cs.typeName))
}
