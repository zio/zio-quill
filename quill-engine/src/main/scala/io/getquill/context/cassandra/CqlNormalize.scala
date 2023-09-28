package io.getquill.context.cassandra

import io.getquill.ast._
import io.getquill.norm.ConcatBehavior.AnsiConcat
import io.getquill.norm.EqualityBehavior.AnsiEquality
import io.getquill.norm.capture.AvoidAliasConflict
import io.getquill.norm.{FlattenOptionOperation, Normalize, RenameProperties, SimplifyNullChecks, TranspileConfig}
import io.getquill.quat.Quat

class CqlNormalize(transpileConfig: TranspileConfig) {
  val NormalizePhase = new Normalize(transpileConfig)

  def apply(ast: Ast): Ast =
    normalize(ast)

  /**
   * Since tuple-elaboration has been removed, need to re-create a similar
   * functionality for the cassandra context since the CqlQuery relies on this.
   */
  private[getquill] def elaborateWithQuat(qry: Ast) =
    qry match {
      case Quat.Is(prodQuat @ Quat.Product(values)) if qry.isInstanceOf[Query] =>
        val id          = Ident("x", prodQuat)
        val tupleValues = values.map { case (k, _) => Property(id, k) }.toList
        Map(qry, id, Tuple(tupleValues))
      case _ =>
        qry
    }

  val RenamePropertiesPhase       = new RenameProperties(transpileConfig.traceConfig)
  val FlattenOptionOperationPhase = new FlattenOptionOperation(AnsiConcat, transpileConfig.traceConfig)
  val SimplifyNullChecksPhase     = new SimplifyNullChecks(AnsiEquality)

  private[this] val normalize =
    (identity[Ast] _)
      .andThen(elaborateWithQuat _)
      .andThen(FlattenOptionOperationPhase.apply _)
      .andThen(SimplifyNullChecksPhase.apply _)
      .andThen(NormalizePhase.apply _)
      .andThen(RenamePropertiesPhase.apply _)
      .andThen(ExpandMappedInfixCassandra.apply _)
      .andThen { ast =>
        // In the final stage of normalization, change all temporary aliases into
        // shorter ones of the form x[0-9]+.
        NormalizePhase.apply(AvoidAliasConflict.Ast(ast, true, transpileConfig.traceConfig))
      }
}
