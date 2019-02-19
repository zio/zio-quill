package io.getquill.context.cassandra

import io.getquill.ast._
import io.getquill.norm.ConcatBehavior.AnsiConcat
import io.getquill.norm.{ FlattenOptionOperation, Normalize, RenameProperties, SimplifyNullChecks }

object CqlNormalize {

  def apply(ast: Ast) =
    normalize(ast)

  private[this] val normalize =
    (identity[Ast] _)
      .andThen(new FlattenOptionOperation(AnsiConcat).apply _)
      .andThen(SimplifyNullChecks.apply _)
      .andThen(Normalize.apply _)
      .andThen(RenameProperties.apply _)
      .andThen(ExpandMappedInfix.apply _)
}
