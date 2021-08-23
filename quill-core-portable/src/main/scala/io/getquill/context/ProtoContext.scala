package io.getquill.context

import io.getquill.{ NamingStrategy, ReturnAction }
import io.getquill.ast.Ast
import scala.language.higherKinds

/**
 * A common context used between Quill and ProtoQuill. This is more like a pre-context because the actual `run`
 * methods cannot be contained here since they use macros. Right now not all Scala2-Quill context extend
 * this context but hopefully they will all in the future. This will establish a common general-api that
 * Quill contexts can use.
 */
trait ProtoContext[Dialect <: io.getquill.idiom.Idiom, Naming <: NamingStrategy] extends RowContext {
  type PrepareRow
  type ResultRow

  type Result[T]
  type RunQuerySingleResult[T]
  type RunQueryResult[T]
  type RunActionResult
  type RunActionReturningResult[T]
  type RunBatchActionResult
  type RunBatchActionReturningResult[T]
  type Session
  /** Future class to hold things like ExecutionContext for Cassandra etc... */
  type DatasourceContext

  def idiom: Dialect
  def naming: Naming

  def executeQuery[T](sql: String, prepare: Prepare, extractor: Extractor[T])(executionInfo: ExecutionInfo, dc: DatasourceContext): Result[RunQueryResult[T]]
  def executeQuerySingle[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(executionInfo: ExecutionInfo, dc: DatasourceContext): Result[RunQuerySingleResult[T]]
  def executeAction[T](sql: String, prepare: Prepare = identityPrepare)(executionInfo: ExecutionInfo, dc: DatasourceContext): Result[RunActionResult]
  def executeActionReturning[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T], returningBehavior: ReturnAction)(executionInfo: ExecutionInfo, dc: DatasourceContext): Result[RunActionReturningResult[T]]
  def executeBatchAction(groups: List[BatchGroup])(executionInfo: ExecutionInfo, dc: DatasourceContext): Result[RunBatchActionResult]
  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(executionInfo: ExecutionInfo, dc: DatasourceContext): Result[RunBatchActionReturningResult[T]]
}

/**
 * Metadata related to query execution. Note that AST should be lazy so as not to be evaluated
 * at runtime (which would happen with a by-value property since `{ ExecutionInfo(stuff, ast) } is spliced
 * into a query-execution site). Additionally, there are performance overheads even splicing the finalized
 * version of the AST into call sites of the `run` functions. For this reason, this functionality
 * is being used only in ProtoQuill and only when a trait extends the trait AstSplicing.
 * In the future it might potentially be controlled by a compiler argument.
 */
class ExecutionInfo(val executionType: ExecutionType, queryAst: => Ast) {
  def ast: Ast = queryAst
}
object ExecutionInfo {
  def apply(executionType: ExecutionType, ast: => Ast) = new ExecutionInfo(executionType, ast)
  val unknown = ExecutionInfo(ExecutionType.Unknown, io.getquill.ast.NullValue)
}

trait AstSplicing

sealed trait ExecutionType
object ExecutionType {
  case object Dynamic extends ExecutionType
  case object Static extends ExecutionType
  case object Unknown extends ExecutionType
}

trait ProtoStreamContext[Dialect <: io.getquill.idiom.Idiom, Naming <: NamingStrategy] extends RowContext {
  type PrepareRow
  type ResultRow

  type DatasourceContext
  type StreamResult[T]
  type Session

  def streamQuery[T](fetchSize: Option[Int], sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: DatasourceContext): StreamResult[T]
}
