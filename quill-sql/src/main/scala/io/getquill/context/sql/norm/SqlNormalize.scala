package io.getquill.context.sql.norm

import io.getquill.norm.FlattenOptionOperation
import io.getquill.norm.Normalize
import io.getquill.ast.Ast
import io.getquill.norm.RenameProperties

object SqlNormalize {

  private val normalize =
    (identity[Ast] _)
      .andThen(FlattenOptionOperation.apply _)
      .andThen(Normalize.apply _)
      .andThen(RenameProperties.apply _)
      .andThen(ExpandJoin.apply _)
      .andThen(Normalize.apply _)

  def apply(ast: Ast) = normalize(ast)
}
