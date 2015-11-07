package io.getquill.source.sql.idiom

object PostgresDialect
    extends SqlIdiom
    with NullsOrderingClause {

  private[idiom] var preparedStatementId = 1

  override def prepare(sql: String) = {
    preparedStatementId += 1
    Some(s"PREPARE p$preparedStatementId AS ${positionalVariables(sql)}")
  }

  private def positionalVariables(sql: String) =
    sql.foldLeft((1, "")) {
      case ((idx, s), '?') =>
        (idx + 1, s + "$" + idx)
      case ((idx, s), c) =>
        (idx, s + c)
    }._2
}
