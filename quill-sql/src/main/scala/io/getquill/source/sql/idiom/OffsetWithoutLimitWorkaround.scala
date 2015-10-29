package io.getquill.source.sql.idiom

import io.getquill.ast.Ast
import io.getquill.util.Show._
import io.getquill.source.sql.naming.NamingStrategy

trait OffsetWithoutLimitWorkaround {
  self: SqlIdiom =>

  override protected def showOffsetWithoutLimit(offset: Ast)(implicit strategy: NamingStrategy) =
    s" LIMIT 18446744073709551610 OFFSET ${offset.show}"
}
