package io.getquill.context

trait ContextVerbPrepareLambda extends ContextVerbPrepare {
  type PrepareQueryResult = Session => Result[PrepareRow]
  type PrepareActionResult = Session => Result[PrepareRow]
  type PrepareBatchActionResult = Session => Result[List[PrepareRow]]
}
