package io.getquill.context.sql.mirror

import io.getquill.context.sql.idiom.SqlIdiom

trait MirrorDialect extends SqlIdiom {
  def prepare(sql: String) = sql
}

object MirrorDialect extends MirrorDialect
