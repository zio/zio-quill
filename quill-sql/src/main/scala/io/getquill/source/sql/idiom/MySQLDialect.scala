package io.getquill.source.sql.idiom

object MySQLDialect
    extends SqlIdiom
    with OffsetWithoutLimitWorkaround {

  override def prepareKeyword = Some("FROM")
}
