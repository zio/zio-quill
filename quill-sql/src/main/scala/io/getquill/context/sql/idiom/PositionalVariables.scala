package io.getquill.context.sql.idiom

trait PositionalVariables { self: SqlIdiom =>

  protected def positionalVariables(sql: String) =
    sql.foldLeft((1, "")) {
      case ((idx, s), '?') =>
        (idx + 1, s + "$" + idx)
      case ((idx, s), c) =>
        (idx, s + c)
    }._2
}
