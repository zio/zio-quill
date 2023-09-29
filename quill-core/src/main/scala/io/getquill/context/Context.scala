package io.getquill.context

import com.typesafe.scalalogging.Logger

import scala.language.higherKinds
import scala.language.experimental.macros
import io.getquill.dsl.CoreDsl
import io.getquill.util.ContextLogger
import io.getquill.util.Messages.fail

import java.io.Closeable
import scala.util.Try
import io.getquill.{Action, ActionReturning, BatchAction, NamingStrategy, Query, Quoted}

trait Context[+Idiom <: io.getquill.idiom.Idiom, +Naming <: NamingStrategy]
    extends RowContext
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
  def run[T](quoted: Quoted[ActionReturning[_, List[T]]]): Result[RunActionReturningResult[List[T]]] =
    macro ActionMacro.runActionReturningMany[T]
  def run[T](quoted: Quoted[ActionReturning[_, T]]): Result[RunActionReturningResult[T]] =
    macro ActionMacro.runActionReturning[T]
  def run(quoted: Quoted[BatchAction[Action[_]]]): Result[RunBatchActionResult] = macro ActionMacro.runBatchAction
  def run(quoted: Quoted[BatchAction[Action[_]]], numRows: Int): Result[RunBatchActionResult] =
    macro ActionMacro.runBatchActionRows
  def run[T](quoted: Quoted[BatchAction[ActionReturning[_, T]]]): Result[RunBatchActionReturningResult[T]] =
    macro ActionMacro.runBatchActionReturning[T]
  def run[T](
    quoted: Quoted[BatchAction[ActionReturning[_, T]]],
    numRows: Int
  ): Result[RunBatchActionReturningResult[T]] = macro ActionMacro.runBatchActionReturningRows[T]

  protected def handleSingleResult[T](sql: String, list: List[T]) =
    list match {
      case Nil =>
        fail(s"Expected a single result from the query: `${sql}` but got a empty result-set!")
      case value :: Nil => value
      case other =>
        io.getquill.log.ContextLog(
          s"Expected a single result from the query: `${sql}` but got: ${abbrevList(other)}. Only the 1st result will be returned!"
        )
        other.head
    }

  private def abbrevList[T](list: List[T]) =
    if (list.length > 10)
      list.take(10).mkString("List(", ",", "...)")
    else
      list.toString()
}
