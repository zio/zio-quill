package io.getquill.source.sql.idiom

object FallbackDialect extends SqlIdiom {
  def prepare(sql: String) = sql
}
