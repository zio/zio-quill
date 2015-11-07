package io.getquill.source.sql.mirror

import io.getquill.source.sql.idiom.SqlIdiom

object MirrorDialect extends SqlIdiom {
  def prepare(sql: String) = sql
}
