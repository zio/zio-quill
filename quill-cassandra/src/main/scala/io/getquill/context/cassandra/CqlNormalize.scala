package io.getquill.context.cassandra

import io.getquill.ast._
import io.getquill.norm.RenameProperties
import io.getquill.norm.Normalize
import io.getquill.norm.FlattenOptionOperation

object CqlNormalize {

  def apply(ast: Ast) =
    normalize(ast)

  private[this] val normalize =
    (identity[Ast] _)
      .andThen(RenameProperties.apply _)
      .andThen(Normalize.apply _)
      .andThen(ExpandMappedInfix.apply _)
      .andThen(FlattenOptionOperation.apply _)
}
