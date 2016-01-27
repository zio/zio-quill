package io.getquill.sources.sql.mirror

import io.getquill.sources.sql.idiom.SqlIdiom

trait MirrorDialect extends SqlIdiom {
  def prepare(sql: String) = sql
}

object MirrorDialect extends MirrorDialect
