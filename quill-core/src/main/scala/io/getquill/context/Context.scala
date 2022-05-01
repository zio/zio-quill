package io.getquill.context

import scala.language.higherKinds
import scala.language.experimental.macros
import io.getquill.dsl.CoreDsl
import io.getquill.util.Messages.fail

import java.io.Closeable
import scala.util.Try
import io.getquill.{ Action, ActionReturning, BatchAction, NamingStrategy, Query, Quoted }

trait Context[Idiom <: io.getquill.idiom.Idiom, Naming <: NamingStrategy] extends RowContext
  with Closeable
  with CoreDsl {

  type Result[T]
  type RunQuerySingleResult[T]
  type RunQueryResult[T]
  type RunActionResult
  type RunActionReturningResult[T]
  type RunBatchActionResult
  type RunBatchActionReturningResult[T]
  type Session
  type Runner

  def probe(statement: String): Try[_]

  def idiom: Idiom
  def naming: Naming

  def run[T](quoted: Quoted[T]): Result[RunQuerySingleResult[T]] = macro QueryMacro.runQuerySingle[T]
  def run[T](quoted: Quoted[Query[T]]): Result[RunQueryResult[T]] = macro QueryMacro.runQuery[T]

  def run(quoted: Quoted[Action[_]]): Result[RunActionResult] = macro ActionMacro.runAction
  def run[T](quoted: Quoted[ActionReturning[_, T]]): Result[RunActionReturningResult[T]] = macro ActionMacro.runActionReturning[T]
  def run(quoted: Quoted[BatchAction[Action[_]]]): Result[RunBatchActionResult] = macro ActionMacro.runBatchAction
  def run[T](quoted: Quoted[BatchAction[ActionReturning[_, T]]]): Result[RunBatchActionReturningResult[T]] = macro ActionMacro.runBatchActionReturning[T]

  protected def handleSingleResult[T](list: List[T]) =
    list match {
      case value :: Nil => value
      case other        => fail(s"Expected a single result but got $other")
    }
}
