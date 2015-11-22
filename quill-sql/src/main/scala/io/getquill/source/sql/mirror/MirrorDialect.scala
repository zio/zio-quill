package io.getquill.source.sql.mirror

import io.getquill.source.sql.idiom.SqlIdiom

trait MirrorDialect extends SqlIdiom {
  def prepare(sql: String) = sql
}

object MirrorDialect extends MirrorDialect
