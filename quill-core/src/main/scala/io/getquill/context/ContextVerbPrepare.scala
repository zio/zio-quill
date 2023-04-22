package io.getquill.context

import io.getquill.{Action, BatchAction, Query, Quoted}
import io.getquill.dsl.CoreDsl
import scala.language.experimental.macros
import scala.language.higherKinds

trait ContextVerbPrepare extends CoreDsl {
  type Result[T]
  type Session

  type PrepareQueryResult       // Usually: Session => Result[PrepareRow]
  type PrepareActionResult      // Usually: Session => Result[PrepareRow]
  type PrepareBatchActionResult // Usually: Session => Result[List[PrepareRow]]

  def prepare[T](quoted: Quoted[Query[T]]): PrepareQueryResult = macro QueryMacro.prepareQuery[T]
  def prepare(quoted: Quoted[Action[_]]): PrepareActionResult = macro ActionMacro.prepareAction
  def prepare(quoted: Quoted[BatchAction[Action[_]]]): PrepareBatchActionResult = macro ActionMacro.prepareBatchAction
}
