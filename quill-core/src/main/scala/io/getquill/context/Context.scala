package io.getquill.context

import scala.language.higherKinds
import scala.language.experimental.macros
import io.getquill.dsl.CoreDsl
import java.io.Closeable
import scala.util.Try
import io.getquill.NamingStrategy

trait Context[Idiom <: io.getquill.idiom.Idiom, Naming <: NamingStrategy]
  extends Closeable
  with CoreDsl {

  type RunQuerySingleResult[T]
  type RunQueryResult[T]
  type RunActionResult
  type RunActionReturningResult[T]
  type RunBatchActionResult
  type RunBatchActionReturningResult[T]

  type Prepare = PrepareRow => (List[Any], PrepareRow)
  type Extractor[T] = ResultRow => T

  case class BatchGroup(string: String, prepare: List[Prepare])
  case class BatchGroupReturning(string: String, column: String, prepare: List[Prepare])

  def probe(statement: String): Try[_]

  def run[T](quoted: Quoted[T]): RunQuerySingleResult[T] = macro QueryMacro.runQuerySingle[T]
  def run[T](quoted: Quoted[Query[T]]): RunQueryResult[T] = macro QueryMacro.runQuery[T]
  def run(quoted: Quoted[Action[_]]): RunActionResult = macro ActionMacro.runAction
  def run[T](quoted: Quoted[ActionReturning[_, T]]): RunActionReturningResult[T] = macro ActionMacro.runActionReturning[T]
  def run(quoted: Quoted[BatchAction[Action[_]]]): RunBatchActionResult = macro ActionMacro.runBatchAction
  def run[T](quoted: Quoted[BatchAction[ActionReturning[_, T]]]): RunBatchActionReturningResult[T] = macro ActionMacro.runBatchActionReturning[T]

  protected val identityPrepare: Prepare = (Nil, _)
  protected val identityExtractor = identity[ResultRow] _

  protected def handleSingleResult[T](list: List[T]) =
    list match {
      case value :: Nil => value
      case other        => throw new IllegalStateException(s"Expected a single result but got $other")
    }
}
