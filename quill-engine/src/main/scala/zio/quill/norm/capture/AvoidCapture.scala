package zio.quill.norm.capture

import zio.quill.ast.Query
import zio.quill.util.TraceConfig

object AvoidCapture {

  def apply(q: Query, traceConfig: TraceConfig): Query =
    Dealias(AvoidAliasConflict(q, false, traceConfig))(traceConfig)
}
