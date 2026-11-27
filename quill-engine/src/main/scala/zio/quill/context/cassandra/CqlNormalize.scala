package zio.quill.context.cassandra

import zio.quill.ast._
import zio.quill.norm.ConcatBehavior.AnsiConcat
import zio.quill.norm.EqualityBehavior.AnsiEquality
import zio.quill.norm.capture.AvoidAliasConflict
import zio.quill.norm.{FlattenOptionOperation, Normalize, RenameProperties, SimplifyNullChecks, TranspileConfig}
import zio.quill.quat.Quat

class CqlNormalize(transpileConfig: TranspileConfig) {
  val NormalizePhase = new Normalize(transpileConfig)

  def apply(ast: Ast) =
    normalize(ast)

  /**
   * Since tuple-elaboration has been removed, need to re-create a similar
   * functionality for the cassandra context since the CqlQuery relies on this.
   */
  private[quill] def elaborateWithQuat(qry: Ast) =
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
