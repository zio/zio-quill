package io.getquill.source.sql.idiom

import io.getquill.ast.Ast
import io.getquill.util.Show._

trait OffsetWithoutLimitWorkaround {
  self: SqlIdiom =>

  override protected def showOffsetWithoutLimit(offset: Ast) =
    s" LIMIT 18446744073709551610 OFFSET ${offset.show}"
}
