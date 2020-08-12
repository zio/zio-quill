package io.getquill.context.cassandra

import io.getquill.ast._
import io.getquill.norm.ConcatBehavior.AnsiConcat
import io.getquill.norm.EqualityBehavior.AnsiEquality
import io.getquill.norm.capture.AvoidAliasConflict
import io.getquill.norm.{ FlattenOptionOperation, Normalize, RenameProperties, SimplifyNullChecks }

object CqlNormalize {

  def apply(ast: Ast) =
    normalize(ast)

  private[this] val normalize =
    (identity[Ast] _)
      .andThen(new FlattenOptionOperation(AnsiConcat).apply _)
      .andThen(new SimplifyNullChecks(AnsiEquality).apply _)
      .andThen(Normalize.apply _)
      .andThen(RenameProperties.apply _)
      .andThen(ExpandMappedInfix.apply _)
      .andThen(ast => {
        // In the final stage of normalization, change all temporary aliases into
        // shorter ones of the form x[0-9]+.
        Normalize.apply(AvoidAliasConflict.Ast(ast, true))
      })
}
