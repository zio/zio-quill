package io.getquill.context.sql.norm

import io.getquill.norm.{SimplifyNullChecks, _}
import io.getquill.ast.Ast
import io.getquill.norm.ConcatBehavior.AnsiConcat
import io.getquill.norm.EqualityBehavior.AnsiEquality
import io.getquill.norm.capture.{AvoidAliasConflict, DemarcateExternalAliases}
import io.getquill.util.Messages.{TraceType, title}
import io.getquill.util.TraceConfig

object SqlNormalize {
  def apply(
    ast: Ast,
    transpileConfig: TranspileConfig,
    concatBehavior: ConcatBehavior = AnsiConcat,
    equalityBehavior: EqualityBehavior = AnsiEquality
  ) =
    new SqlNormalize(concatBehavior, equalityBehavior, transpileConfig)(ast)
}

class SqlNormalize(
  concatBehavior: ConcatBehavior,
  equalityBehavior: EqualityBehavior,
  transpileConfig: TranspileConfig
) {

  val NormalizePhase = new Normalize(transpileConfig)
  val traceConfig    = transpileConfig.traceConfig

  private def demarcate(heading: String) =
    ((ast: Ast) => title(heading, TraceType.SqlNormalizations)(ast))

  val ExpandJoinPhase             = new ExpandJoin(NormalizePhase)
  val RenamePropertiesPhase       = new RenameProperties(traceConfig)
  val ExpandDistinctPhase         = new ExpandDistinct(traceConfig)
  val SheathLeafClausesPhase      = new SheathLeafClausesApply(traceConfig)
  val FlattenOptionOperationPhase = new FlattenOptionOperation(concatBehavior, transpileConfig.traceConfig)
  val SimplifyNullChecksPhase     = new SimplifyNullChecks(equalityBehavior)

  private val normalize =
    (identity[Ast] _)
      .andThen(demarcate("original"))
      .andThen(DemarcateExternalAliases.apply _)
      .andThen(demarcate("DemarcateReturningAliases"))
      .andThen(FlattenOptionOperationPhase.apply _)
      .andThen(demarcate("FlattenOptionOperation"))
      .andThen(SimplifyNullChecksPhase.apply _)
      .andThen(demarcate("SimplifyNullChecks"))
      .andThen(NormalizePhase.apply _)
      .andThen(demarcate("Normalize"))
      // Need to do RenameProperties before ExpandJoin which normalizes-out all the tuple indexes
      // on which RenameProperties relies
      // .andThen(RenameProperties.apply _)
      .andThen(RenamePropertiesPhase.apply _)
      .andThen(demarcate("RenameProperties"))
      .andThen(ExpandDistinctPhase.apply _)
      .andThen(demarcate("ExpandDistinct"))
      .andThen(NormalizePhase.apply _)
      .andThen(demarcate("Normalize")) // Needed only because ExpandDistinct introduces an alias.
      .andThen(NormalizePhase.apply _)
      .andThen(demarcate("Normalize"))
      .andThen(ExpandJoinPhase.apply _)
      .andThen(demarcate("ExpandJoin"))
      .andThen(ExpandMappedInfix.apply _)
      .andThen(demarcate("ExpandMappedInfix"))
      .andThen(SheathLeafClausesPhase.apply _)
      .andThen(demarcate("SheathLeaves"))
      .andThen { ast =>
        // In the final stage of normalization, change all temporary aliases into
        // shorter ones of the form x[0-9]+.
        NormalizePhase.apply(AvoidAliasConflict.Ast(ast, true, transpileConfig.traceConfig))
      }
      .andThen(demarcate("Normalize"))

  def apply(ast: Ast) = normalize(ast)
}
