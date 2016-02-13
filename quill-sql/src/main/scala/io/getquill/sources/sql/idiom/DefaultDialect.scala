package io.getquill.sources.sql.idiom

object FallbackDialect extends SqlIdiom {
  def prepare(sql: String) = sql
}
