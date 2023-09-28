package io.getquill.norm

import io.getquill.norm.ConfigList._
import io.getquill.util.Messages.TraceType
import io.getquill.util.TraceConfig

final case class TranspileConfig(disablePhases: List[OptionalPhase], traceConfig: TraceConfig)
object TranspileConfig {
  val Empty: TranspileConfig = TranspileConfig(List.empty, TraceConfig(List.empty))
}

sealed trait OptionalPhase
object OptionalPhase {
  sealed trait ApplyMap extends OptionalPhase
  case object ApplyMap  extends ApplyMap

  val all: List[ApplyMap.type] = List(ApplyMap)
}

trait DisablePhase {
  type Phase <: HList[OptionalPhase]
}

object DisablePhaseNone extends DisablePhase {
  type Phase = HNil
}

trait EnableTrace {
  type Trace <: HList[TraceType]
}

object EnableTraceNone extends EnableTrace {
  type Trace = HNil
}

object ConfigList {
  sealed trait HList[+H]
  final case class ::[+H, +T <: HList[_]](head: H, tail: T) extends HList[H]
  sealed trait HNil extends HList[Nothing] {
    def ::[H](h: H) = ConfigList.::[H, HNil](h, this)
  }
  case object HNil extends HNil
}
