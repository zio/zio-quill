package zio.quill.quotation

import zio.quill.IdiomContext
import zio.quill.IdiomContext.QueryType
import zio.quill.IdiomContext.QueryType.{Batch, Regular}
import zio.quill.context.ExecutionType
import zio.quill.norm.{OptionalPhase, TranspileConfig}
import zio.quill.util.Messages.TraceType
import zio.quill.util.TraceConfig
import zio.quill.util.MacroContextExt._

import scala.reflect.macros.whitebox.Context

trait TranspileConfigSummoning {
  val c: Context
  import c.universe._

  private[quill] lazy val transpileConfig = summonTranspileConfig()

  protected def summonTranspileConfig(): TranspileConfig = {
    val enabledTraces   = summonEnabledTraces()
    val transpileConfig = summonPhaseDisable()
    TranspileConfig(transpileConfig, TraceConfig(enabledTraces))
  }

  private def parseSealedTraitClassName(cls: Class[_]) =
    cls.getName.stripSuffix("$").replaceFirst("(.*)[\\.$]", "")

  protected def summonEnabledTraces(): List[TraceType] = {
    val enableTraceTpe         = c.typecheck(tq"zio.quill.norm.EnableTrace", c.TYPEmode).tpe
    val enableTrace            = c.inferImplicitValue(enableTraceTpe).orElse(q"zio.quill.norm.EnableTraceNone")
    val enableTraceSummonedTpe = c.typecheck(enableTrace).tpe
    val traceMemberOpt         =
      enableTraceSummonedTpe.members.find(_.name.toString == "Trace").map(_.typeSignatureIn(enableTraceSummonedTpe))
    traceMemberOpt match {
      case Some(value) =>
        val configListMembers = getConfigListMembers(value)
        val foundMemberNames  = configListMembers.map(_.typeSymbol.name.toString)
        TraceType.values.filter { trace =>
          val simpleName = parseSealedTraitClassName(trace.getClass)
          foundMemberNames.contains(simpleName)
        }
      case None =>
        List.empty
    }
  }

  protected def summonPhaseDisable(): List[OptionalPhase] = {
    val disablePhaseTpe         = c.typecheck(tq"zio.quill.norm.DisablePhase", c.TYPEmode).tpe
    val disablePhase            = c.inferImplicitValue(disablePhaseTpe).orElse(q"zio.quill.norm.DisablePhaseNone")
    val disablePhaseSummonedTpe = c.typecheck(disablePhase).tpe
    val phaseMemberOpt          =
      disablePhaseSummonedTpe.members.find(_.name.toString == "Phase").map(_.typeSignatureIn(disablePhaseSummonedTpe))
    phaseMemberOpt match {
      case Some(value) =>
        val configListMembers = getConfigListMembers(value)
        val foundMemberNames  = configListMembers.map(_.typeSymbol.name.toString)
        OptionalPhase.all.filter { phase =>
          val simpleName = parseSealedTraitClassName(phase.getClass)
          foundMemberNames.contains(simpleName)
        }
      case None =>
        List.empty
    }
  }

  private[quill] def getConfigListMembers(consMember: Type): List[Type] = {
    val isNil  = consMember <:< typeOf[zio.quill.norm.ConfigList.HNil]
    val isCons = consMember <:< typeOf[zio.quill.norm.ConfigList.::[_, _]]
    if (isNil) Nil
    else if (isCons) {
      val member = consMember.typeArgs(0)
      val next   = consMember.typeArgs(1)
      member :: getConfigListMembers(next)
    } else {
      c.warn(s"Unknown parameter of ConfigList ${consMember} is not a HList Cons or Nil. Ignoring it.")
      Nil
    }
  }

  object ConfigLiftables {
    implicit val optionalPhaseLiftable: Liftable[OptionalPhase] = Liftable[OptionalPhase] {
      case OptionalPhase.ApplyMap => q"zio.quill.norm.OptionalPhase.ApplyMap"
    }

    implicit val traceTypeLiftable: Liftable[TraceType] = Liftable[TraceType] {
      case TraceType.SqlNormalizations      => q"zio.quill.util.Messages.TraceType.SqlNormalizations"
      case TraceType.ExpandDistinct         => q"zio.quill.util.Messages.TraceType.ExpandDistinct"
      case TraceType.Normalizations         => q"zio.quill.util.Messages.TraceType.Normalizations"
      case TraceType.Standard               => q"zio.quill.util.Messages.TraceType.Standard"
      case TraceType.NestedQueryExpansion   => q"zio.quill.util.Messages.TraceType.NestedQueryExpansion"
      case TraceType.AvoidAliasConflict     => q"zio.quill.util.Messages.TraceType.AvoidAliasConflict"
      case TraceType.ShealthLeaf            => q"zio.quill.util.Messages.TraceType.ShealthLeaf"
      case TraceType.ReifyLiftings          => q"zio.quill.util.Messages.TraceType.ReifyLiftings"
      case TraceType.PatMatch               => q"zio.quill.util.Messages.TraceType.PatMatch"
      case TraceType.Quotation              => q"zio.quill.util.Messages.TraceType.Quotation"
      case TraceType.RepropagateQuats       => q"zio.quill.util.Messages.TraceType.RepropagateQuats"
      case TraceType.RenameProperties       => q"zio.quill.util.Messages.TraceType.RenameProperties"
      case TraceType.ApplyMap               => q"zio.quill.util.Messages.TraceType.ApplyMap"
      case TraceType.Warning                => q"zio.quill.util.Messages.TraceType.Warning"
      case TraceType.ExprModel              => q"zio.quill.util.Messages.TraceType.ExprModel"
      case TraceType.Meta                   => q"zio.quill.util.Messages.TraceType.Meta"
      case TraceType.Execution              => q"zio.quill.util.Messages.TraceType.Execution"
      case TraceType.DynamicExecution       => q"zio.quill.util.Messages.TraceType.DynamicExecution"
      case TraceType.Elaboration            => q"zio.quill.util.Messages.TraceType.Elaboration"
      case TraceType.SqlQueryConstruct      => q"zio.quill.util.Messages.TraceType.SqlQueryConstruct"
      case TraceType.FlattenOptionOperation => q"zio.quill.util.Messages.TraceType.FlattenOptionOperation"
      case TraceType.Particularization      => q"zio.quill.util.Messages.TraceType.Particularization"
    }

    implicit val traceConfigLiftable: Liftable[TraceConfig] = Liftable[TraceConfig] { case TraceConfig(enabledTraces) =>
      q"zio.quill.util.TraceConfig(${enabledTraces})"
    }

    implicit val transpileConfigLiftable: Liftable[TranspileConfig] = Liftable[TranspileConfig] {
      case TranspileConfig(disablePhases, traceConfig) =>
        q"zio.quill.norm.TranspileConfig(${disablePhases}, ${traceConfig})"
    }

    implicit val queryTypeRegularLiftable: Liftable[Regular] = Liftable[Regular] {
      case QueryType.Select => q"zio.quill.IdiomContext.QueryType.Select"
      case QueryType.Insert => q"zio.quill.IdiomContext.QueryType.Insert"
      case QueryType.Update => q"zio.quill.IdiomContext.QueryType.Update"
      case QueryType.Delete => q"zio.quill.IdiomContext.QueryType.Delete"
    }

    implicit val queryTypeBatchLiftable: Liftable[Batch] = Liftable[Batch] {
      case QueryType.BatchInsert(foreachAlias) => q"zio.quill.IdiomContext.QueryType.BatchInsert($foreachAlias)"
      case QueryType.BatchUpdate(foreachAlias) => q"zio.quill.IdiomContext.QueryType.BatchUpdate($foreachAlias)"
    }

    implicit val queryTypeLiftable: Liftable[QueryType] = Liftable[QueryType] {
      case v: Regular => queryTypeRegularLiftable(v)
      case v: Batch   => queryTypeBatchLiftable(v)
    }

    implicit val transpileContextLiftable: Liftable[IdiomContext] = Liftable[IdiomContext] {
      case IdiomContext(transpileConfig, queryType) => q"zio.quill.IdiomContext(${transpileConfig}, ${queryType})"
    }

    implicit val executionTypeLiftable: Liftable[ExecutionType] = Liftable[ExecutionType] {
      case ExecutionType.Dynamic => q"zio.quill.context.ExecutionType.Dynamic"
      case ExecutionType.Static  => q"zio.quill.context.ExecutionType.Static"
      case ExecutionType.Unknown => q"zio.quill.context.ExecutionType.Unknown"
    }
  }
}
