package io.getquill.context.sql.norm

import io.getquill.norm.FlattenOptionOperation
import io.getquill.norm.Normalize
import io.getquill.ast.Ast
import io.getquill.norm.RenameProperties
import io.getquill.util.Messages.trace

object SqlNormalize {

  private val normalize =
    (identity[Ast] _)
      .andThen(trace("original"))
      .andThen(FlattenOptionOperation.apply _)
      .andThen(trace("FlattenOptionOperation"))
      .andThen(Normalize.apply _)
      .andThen(trace("Normalize"))
      .andThen(RenameProperties.apply _)
      .andThen(trace("RenameProperties"))
      .andThen(ExpandJoin.apply _)
      .andThen(trace("ExpandJoin"))
      .andThen(Normalize.apply _)
      .andThen(trace("Normalize"))

  def apply(ast: Ast) = normalize(ast)
}
