package zio.quill.norm

import zio.quill.ast.Ast
import zio.quill.ast.Query
import zio.quill.ast.StatelessTransformer
import zio.quill.norm.capture.{AvoidAliasConflictApply, DealiasApply}
import zio.quill.ast.Action
import zio.quill.util.Interpolator
import zio.quill.util.Messages.{TraceType, title, trace}
import zio.quill.util.Messages.TraceType.Normalizations

import scala.annotation.tailrec

class Normalize(config: TranspileConfig) extends StatelessTransformer {

  val traceConf = config.traceConfig
  val interp    = new Interpolator(TraceType.Normalizations, traceConf, 1)
  import interp._

  // These are the actual phases of core-normalization. Highlighting them as such.
  lazy val NormalizeReturningPhase   = new NormalizeReturning(this)
  lazy val DealiasPhase              = new DealiasApply(traceConf)
  lazy val AvoidAliasConflictPhase   = new AvoidAliasConflictApply(traceConf)
  val NormalizeNestedStructuresPhase = new NormalizeNestedStructures(this)
  val SymbolicReductionPhase         = new SymbolicReduction(traceConf)
  val AdHocReductionPhase            = new AdHocReduction(traceConf)
  val OrderTermsPhase                = new OrderTerms(traceConf)

  override def apply(q: Ast): Ast =
    super.apply(BetaReduction(q))

  override def apply(q: Action): Action =
    NormalizeReturningPhase(super.apply(q))

  override def apply(q: Query): Query =
    trace"Avoid Capture and Normalize $q into:" andReturn
      norm(DealiasPhase(AvoidAliasConflictPhase(q, false)))

  private def traceNorm[T](label: String) =
    trace[T](s"${label} (Normalize)", 1, Normalizations)

  private def demarcate(heading: String) =
    ((ast: Query) => title(s"(Normalize) $heading", TraceType.Normalizations)(ast))

  // Create an ApplyMap phase-instance that will always return None if configured to be disabled.
  // This is a simple way of not running the phase at all if it is not configured to run.
  object ApplyMapPhase {
    // For logging that ApplyMap has been disabled
    val applyMapInterp                       = new Interpolator(TraceType.ApplyMap, traceConf, 1)
    val applyMapInstance                     = new ApplyMap(traceConf)
    def unapply(query: Query): Option[Query] =
      if (config.disablePhases.contains(OptionalPhase.ApplyMap)) {
        import applyMapInterp._
        trace"ApplyMap phase disabled. Not executing on: $query".andLog()
        None
      } else {
        applyMapInstance.unapply(query)
      }
  }

  @tailrec
  private def norm(q: Query): Query =
    q match {
      case NormalizeNestedStructuresPhase(query) =>
        demarcate("NormalizeNestedStructures")(query)
        norm(query)
      case ApplyMapPhase(query) =>
        demarcate("ApplyMap")(query)
        norm(query)
      case SymbolicReductionPhase(query) =>
        demarcate("SymbolicReduction")(query)
        norm(query)
      case AdHocReductionPhase(query) =>
        demarcate("AdHocReduction")(query)
        norm(query)
      case OrderTermsPhase(query) =>
        demarcate("OrderTerms")(query)
        norm(query)
      case other =>
        other
    }
}
