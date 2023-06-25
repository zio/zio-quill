package io.getquill.norm.capture

import io.getquill.ast.Query
import io.getquill.util.TraceConfig

object AvoidCapture {

  def apply(q: Query, traceConfig: TraceConfig): Query =
    Dealias(AvoidAliasConflict(q, false, traceConfig))(traceConfig)
}
