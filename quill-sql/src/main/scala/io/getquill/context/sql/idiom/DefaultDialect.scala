package io.getquill.context.sql.idiom

object FallbackDialect extends SqlIdiom {
  def prepare(sql: String) = sql
}
