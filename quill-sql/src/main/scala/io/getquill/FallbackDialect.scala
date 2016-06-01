package io.getquill

import io.getquill.context.sql.idiom.SqlIdiom

object FallbackDialect extends SqlIdiom {
  def prepare(sql: String) = sql
}
