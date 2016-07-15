package io.getquill

import io.getquill.context.sql.idiom.SqlIdiom

trait SqliteDialect
  extends SqlIdiom {

  override def prepare(sql: String) = s"sqlite3_prepare_v2($sql)"
}

object SqliteDialect extends SqliteDialect
