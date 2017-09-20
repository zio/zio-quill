package io.getquill.context.sql.idiom

import io.getquill.idiom.StatementInterpolator._
import io.getquill.ast.Ast
import io.getquill.NamingStrategy

trait OffsetWithoutLimitWorkaround {
  self: SqlIdiom =>

  override protected def tokenOffsetWithoutLimit(offset: Ast)(implicit astTokernizer: Tokenizer[Ast], strategy: NamingStrategy) =
    stmt" LIMIT 18446744073709551610 OFFSET ${offset.token}"
}
