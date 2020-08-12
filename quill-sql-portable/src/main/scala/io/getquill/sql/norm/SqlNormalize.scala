package io.getquill.context.sql.norm

import io.getquill.norm._
import io.getquill.ast.Ast
import io.getquill.norm.ConcatBehavior.AnsiConcat
import io.getquill.norm.EqualityBehavior.AnsiEquality
import io.getquill.norm.capture.{ AvoidAliasConflict, DemarcateExternalAliases }
import io.getquill.util.Messages.{ TraceType, title }

object SqlNormalize {
  def apply(ast: Ast, concatBehavior: ConcatBehavior = AnsiConcat, equalityBehavior: EqualityBehavior = AnsiEquality) =
    new SqlNormalize(concatBehavior, equalityBehavior)(ast)
}

class SqlNormalize(concatBehavior: ConcatBehavior, equalityBehavior: EqualityBehavior) {

  private def demarcate(heading: String) =
    ((ast: Ast) => title(heading, TraceType.SqlNormalizations)(ast))

  private val normalize =
    (identity[Ast] _)
      .andThen(demarcate("original"))
      .andThen(DemarcateExternalAliases.apply _)
      .andThen(demarcate("DemarcateReturningAliases"))
      .andThen(new FlattenOptionOperation(concatBehavior).apply _)
      .andThen(demarcate("FlattenOptionOperation"))
      .andThen(new SimplifyNullChecks(equalityBehavior).apply _)
      .andThen(demarcate("SimplifyNullChecks"))
      .andThen(Normalize.apply _)
      .andThen(demarcate("Normalize"))
      // Need to do RenameProperties before ExpandJoin which normalizes-out all the tuple indexes
      // on which RenameProperties relies
      //.andThen(RenameProperties.apply _)
      .andThen(RenameProperties.apply _)
      .andThen(demarcate("RenameProperties"))
      .andThen(ExpandDistinct.apply _)
      .andThen(demarcate("ExpandDistinct"))
      .andThen(Normalize.apply _)
      .andThen(demarcate("Normalize")) // Needed only because ExpandDistinct introduces an alias.
      .andThen(NestImpureMappedInfix.apply _)
      .andThen(demarcate("NestImpureMappedInfix"))
      .andThen(Normalize.apply _)
      .andThen(demarcate("Normalize"))
      .andThen(ExpandJoin.apply _)
      .andThen(demarcate("ExpandJoin"))
      .andThen(ExpandMappedInfix.apply _)
      .andThen(demarcate("ExpandMappedInfix"))
      .andThen(ast => {
        // In the final stage of normalization, change all temporary aliases into
        // shorter ones of the form x[0-9]+.
        Normalize.apply(AvoidAliasConflict.Ast(ast, true))
      })
      .andThen(demarcate("Normalize"))

  def apply(ast: Ast) = normalize(ast)
}
