package io.getquill.quotation

import io.getquill.IdiomContext
import io.getquill.IdiomContext.QueryType
import io.getquill.IdiomContext.QueryType.{ Batch, Regular }
import io.getquill.context.ExecutionType
import io.getquill.norm.{ OptionalPhase, TranspileConfig }
import io.getquill.util.Messages.TraceType
import io.getquill.util.TraceConfig
import io.getquill.util.MacroContextExt._

import scala.reflect.macros.whitebox.Context

trait TranspileConfigSummoning {
  val c: Context
  import c.universe._

  private[getquill] lazy val transpileConfig = summonTranspileConfig()

  protected def summonTranspileConfig(): TranspileConfig = {
    val enabledTraces = summonEnabledTraces()
    val transpileConfig = summonPhaseDisable()
    TranspileConfig(transpileConfig, TraceConfig(enabledTraces))
  }

  private def parseSealedTraitClassName(cls: Class[_]) =
    cls.getName.stripSuffix("$").replaceFirst("(.*)[\\.$]", "")

  protected def summonEnabledTraces(): List[TraceType] = {
    val enableTraceTpe = c.typecheck(tq"io.getquill.norm.EnableTrace", c.TYPEmode).tpe
    val enableTrace = c.inferImplicitValue(enableTraceTpe).orElse(q"io.getquill.norm.EnableTraceNone")
    val enableTraceSummonedTpe = c.typecheck(enableTrace).tpe
    val traceMemberOpt = enableTraceSummonedTpe.members.find(_.name.toString == "Trace").map(_.typeSignatureIn(enableTraceSummonedTpe))
    traceMemberOpt match {
      case Some(value) =>
        val configListMembers = getConfigListMembers(value)
        val foundMemberNames = configListMembers.map(_.typeSymbol.name.toString)
        TraceType.values.filter { trace =>
          val simpleName = parseSealedTraitClassName(trace.getClass)
          foundMemberNames.contains(simpleName)
        }
      case None =>
        List()
    }
  }

  protected def summonPhaseDisable(): List[OptionalPhase] = {
    val disablePhaseTpe = c.typecheck(tq"io.getquill.norm.DisablePhase", c.TYPEmode).tpe
    val disablePhase = c.inferImplicitValue(disablePhaseTpe).orElse(q"io.getquill.norm.DisablePhaseNone")
    val disablePhaseSummonedTpe = c.typecheck(disablePhase).tpe
    val phaseMemberOpt = disablePhaseSummonedTpe.members.find(_.name.toString == "Phase").map(_.typeSignatureIn(disablePhaseSummonedTpe))
    phaseMemberOpt match {
      case Some(value) =>
        val configListMembers = getConfigListMembers(value)
        val foundMemberNames = configListMembers.map(_.typeSymbol.name.toString)
        OptionalPhase.all.filter { phase =>
          val simpleName = parseSealedTraitClassName(phase.getClass)
          foundMemberNames.contains(simpleName)
        }
      case None =>
        List()
    }
  }

  private[getquill] def getConfigListMembers(consMember: Type): List[Type] = {
    val isNil = consMember <:< typeOf[io.getquill.norm.ConfigList.HNil]
    val isCons = consMember <:< typeOf[io.getquill.norm.ConfigList.::[_, _]]
    if (isNil) Nil
    else if (isCons) {
      val member = consMember.typeArgs(0)
      val next = consMember.typeArgs(1)
      member :: getConfigListMembers(next)
    } else {
      c.warn(s"Unknown parameter of ConfigList ${consMember} is not a HList Cons or Nil. Ignoring it.")
      Nil
    }
  }

  object ConfigLiftables {
    implicit val optionalPhaseLiftable: Liftable[OptionalPhase] = Liftable[OptionalPhase] {
      case OptionalPhase.ApplyMap => q"io.getquill.norm.OptionalPhase.ApplyMap"
    }

    implicit val traceTypeLiftable: Liftable[TraceType] = Liftable[TraceType] {
      case TraceType.SqlNormalizations      => q"io.getquill.util.Messages.TraceType.SqlNormalizations"
      case TraceType.ExpandDistinct         => q"io.getquill.util.Messages.TraceType.ExpandDistinct"
      case TraceType.Normalizations         => q"io.getquill.util.Messages.TraceType.Normalizations"
      case TraceType.Standard               => q"io.getquill.util.Messages.TraceType.Standard"
      case TraceType.NestedQueryExpansion   => q"io.getquill.util.Messages.TraceType.NestedQueryExpansion"
      case TraceType.AvoidAliasConflict     => q"io.getquill.util.Messages.TraceType.AvoidAliasConflict"
      case TraceType.ShealthLeaf            => q"io.getquill.util.Messages.TraceType.ShealthLeaf"
      case TraceType.ReifyLiftings          => q"io.getquill.util.Messages.TraceType.ReifyLiftings"
      case TraceType.PatMatch               => q"io.getquill.util.Messages.TraceType.PatMatch"
      case TraceType.Quotation              => q"io.getquill.util.Messages.TraceType.Quotation"
      case TraceType.RepropagateQuats       => q"io.getquill.util.Messages.TraceType.RepropagateQuats"
      case TraceType.RenameProperties       => q"io.getquill.util.Messages.TraceType.RenameProperties"
      case TraceType.ApplyMap               => q"io.getquill.util.Messages.TraceType.ApplyMap"
      case TraceType.Warning                => q"io.getquill.util.Messages.TraceType.Warning"
      case TraceType.ExprModel              => q"io.getquill.util.Messages.TraceType.ExprModel"
      case TraceType.Meta                   => q"io.getquill.util.Messages.TraceType.Meta"
      case TraceType.Execution              => q"io.getquill.util.Messages.TraceType.Execution"
      case TraceType.DynamicExecution       => q"io.getquill.util.Messages.TraceType.DynamicExecution"
      case TraceType.Elaboration            => q"io.getquill.util.Messages.TraceType.Elaboration"
      case TraceType.SqlQueryConstruct      => q"io.getquill.util.Messages.TraceType.SqlQueryConstruct"
      case TraceType.FlattenOptionOperation => q"io.getquill.util.Messages.TraceType.FlattenOptionOperation"
      case TraceType.Particularization      => q"io.getquill.util.Messages.TraceType.Particularization"
    }

    implicit val traceConfigLiftable: Liftable[TraceConfig] = Liftable[TraceConfig] {
      case TraceConfig(enabledTraces) => q"io.getquill.util.TraceConfig(${enabledTraces})"
    }

    implicit val transpileConfigLiftable: Liftable[TranspileConfig] = Liftable[TranspileConfig] {
      case TranspileConfig(disablePhases, traceConfig) => q"io.getquill.norm.TranspileConfig(${disablePhases}, ${traceConfig})"
    }

    implicit val queryTypeRegularLiftable: Liftable[Regular] = Liftable[Regular] {
      case QueryType.Select => q"io.getquill.IdiomContext.QueryType.Select"
      case QueryType.Insert => q"io.getquill.IdiomContext.QueryType.Insert"
      case QueryType.Update => q"io.getquill.IdiomContext.QueryType.Update"
      case QueryType.Delete => q"io.getquill.IdiomContext.QueryType.Delete"
    }

    implicit val queryTypeBatchLiftable: Liftable[Batch] = Liftable[Batch] {
      case QueryType.BatchInsert(foreachAlias) => q"io.getquill.IdiomContext.QueryType.BatchInsert($foreachAlias)"
      case QueryType.BatchUpdate(foreachAlias) => q"io.getquill.IdiomContext.QueryType.BatchUpdate($foreachAlias)"
    }

    implicit val queryTypeLiftable: Liftable[QueryType] = Liftable[QueryType] {
      case v: Regular => queryTypeRegularLiftable(v)
      case v: Batch   => queryTypeBatchLiftable(v)
    }

    implicit val transpileContextLiftable: Liftable[IdiomContext] = Liftable[IdiomContext] {
      case IdiomContext(transpileConfig, queryType) => q"io.getquill.IdiomContext(${transpileConfig}, ${queryType})"
    }

    implicit val executionTypeLiftable: Liftable[ExecutionType] = Liftable[ExecutionType] {
      case ExecutionType.Dynamic => q"io.getquill.context.ExecutionType.Dynamic"
      case ExecutionType.Static  => q"io.getquill.context.ExecutionType.Static"
      case ExecutionType.Unknown => q"io.getquill.context.ExecutionType.Unknown"
    }
  }
}