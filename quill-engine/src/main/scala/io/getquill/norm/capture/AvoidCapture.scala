package io.getquill.norm.capture

import io.getquill.NoCache
import io.getquill.ast.Query
import io.getquill.util.TraceConfig

// This is not actually used anywhere but there is a test that tests
// the general functionality of dealiasing. Move this to there
object AvoidCapture {

  def apply(q: Query, traceConfig: TraceConfig): Query =
    Dealias(AvoidAliasConflict(q, false, traceConfig))(traceConfig, new NoCache)
}
