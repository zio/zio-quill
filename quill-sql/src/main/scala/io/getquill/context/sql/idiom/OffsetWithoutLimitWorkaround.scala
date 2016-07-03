package io.getquill.context.sql.idiom

import io.getquill.ast.Ast
import io.getquill.naming.NamingStrategy
import io.getquill.util.Show.Shower

trait OffsetWithoutLimitWorkaround {
  self: SqlIdiom =>

  override protected def showOffsetWithoutLimit(offset: Ast)(implicit strategy: NamingStrategy) =
    s" LIMIT 18446744073709551610 OFFSET ${offset.show}"
}
