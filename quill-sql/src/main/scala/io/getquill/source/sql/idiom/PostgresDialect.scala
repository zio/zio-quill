package io.getquill.source.sql.idiom

object PostgresDialect
    extends SqlIdiom
    with NullsOrderingClause {

  override def prepare(sql: String) =
    Some(s"PREPARE p${sql.hashCode.abs} AS $sql")
}
