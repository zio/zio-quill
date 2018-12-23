package io.getquill.context.monix

import java.sql.{ PreparedStatement, ResultSet }

import io.getquill.NamingStrategy
import io.getquill.context.{ Context, StreamingContext }
import monix.eval.Task
import monix.reactive.Observable

trait MonixContext[Idiom <: io.getquill.idiom.Idiom, Naming <: NamingStrategy] extends Context[Idiom, Naming]
  with StreamingContext[Idiom, Naming]
  with MonixTranslateContext {

  override type PrepareRow = PreparedStatement
  override type ResultRow = ResultSet
  override type StreamResult[T] = Observable[T]
  override type Result[T] = Task[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  // Need explicit return-type annotations due to scala/bug#8356. Otherwise macro system will not understand Result[Long]=Task[Long] etc...
  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Task[List[T]]
  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Task[T]
  def executeAction[T](sql: String, prepare: Prepare = identityPrepare): Task[Long]
  def executeBatchAction(groups: List[BatchGroup]): Task[List[Long]]

  private[getquill] val effect: Runner
}
