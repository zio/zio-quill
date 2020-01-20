package io.getquill.context.sql.norm

import io.getquill.norm._
import io.getquill.ast.Ast
import io.getquill.norm.ConcatBehavior.AnsiConcat
import io.getquill.norm.EqualityBehavior.AnsiEquality
import io.getquill.norm.capture.DemarcateExternalAliases
import io.getquill.util.Messages.title

object SqlNormalize {
  def apply(ast: Ast, concatBehavior: ConcatBehavior = AnsiConcat, equalityBehavior: EqualityBehavior = AnsiEquality) =
    new SqlNormalize(concatBehavior, equalityBehavior)(ast)
}

class SqlNormalize(concatBehavior: ConcatBehavior, equalityBehavior: EqualityBehavior) {

  private val normalize =
    (identity[Ast] _)
      .andThen(title("original"))
      .andThen(DemarcateExternalAliases.apply _)
      .andThen(title("DemarcateReturningAliases"))
      .andThen(new FlattenOptionOperation(concatBehavior).apply _)
      .andThen(title("FlattenOptionOperation"))
      .andThen(new SimplifyNullChecks(equalityBehavior).apply _)
      .andThen(title("SimplifyNullChecks"))
      .andThen(Normalize.apply _)
      .andThen(title("Normalize"))
      // Need to do RenameProperties before ExpandJoin which normalizes-out all the tuple indexes
      // on which RenameProperties relies
      .andThen(RenameProperties.apply _)
      .andThen(title("RenameProperties"))
      .andThen(ExpandDistinct.apply _)
      .andThen(title("ExpandDistinct"))
      .andThen(NestImpureMappedInfix.apply _)
      .andThen(title("NestMappedInfix"))
      .andThen(Normalize.apply _)
      .andThen(title("Normalize"))
      .andThen(ExpandJoin.apply _)
      .andThen(title("ExpandJoin"))
      .andThen(ExpandMappedInfix.apply _)
      .andThen(title("ExpandMappedInfix"))
      .andThen(Normalize.apply _)
      .andThen(title("Normalize"))

  def apply(ast: Ast) = normalize(ast)
}
