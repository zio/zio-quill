package io.getquill.monad

import io.getquill.context.Context
import io.getquill.context.mirror.Row
import scala.concurrent.Future
import io.getquill.context.mirror.MirrorEncoders
import io.getquill.context.mirror.MirrorDecoders
import io.getquill.MirrorIdiom
import io.getquill.NamingStrategy
import io.getquill.testContext

class AsyncMirrorContext
  extends Context[MirrorIdiom, NamingStrategy]
  with MirrorEncoders
  with MirrorDecoders
  with ScalaFutureIOMonad {

  override type PrepareRow = Row
  override type ResultRow = Row

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = testContext.QueryMirror[T]
  override type RunQuerySingleResult[T] = testContext.QueryMirror[T]
  override type RunActionResult = testContext.ActionMirror
  override type RunActionReturningResult[T] = testContext.ActionReturningMirror[T]
  override type RunBatchActionResult = testContext.BatchActionMirror
  override type RunBatchActionReturningResult[T] = testContext.BatchActionReturningMirror[T]

  override def close = ()

  def probe(statement: String) = testContext.probe(statement)

  def transaction[T](f: => T) = testContext.transaction(f)

  def executeQuery[T](string: String, prepare: Prepare = identityPrepare, extractor: Row => T = identity[Row] _) =
    Future.successful(testContext.executeQuery(string, prepare, extractor))

  def executeQuerySingle[T](string: String, prepare: Prepare = identityPrepare, extractor: Row => T = identity[Row] _) =
    Future.successful(testContext.executeQuerySingle(string, prepare, extractor))

  def executeAction(string: String, prepare: Prepare = identityPrepare) =
    Future.successful(testContext.executeAction(string, prepare))

  def executeActionReturning[O](string: String, prepare: Prepare = identityPrepare, extractor: Row => O,
                                returningColumn: String) =
    Future.successful(testContext.executeActionReturning(string, prepare, extractor, returningColumn))

  def executeBatchAction(groups: List[BatchGroup]) =
    Future.successful(testContext.executeBatchAction(convertBatch(groups)))

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Row => T) =
    Future.successful(testContext.executeBatchActionReturning(convertBatchReturning(groups), extractor))

  private def convertBatch(g: List[BatchGroup]) =
    g.map(b => testContext.BatchGroup(b.string, b.prepare))

  private def convertBatchReturning(g: List[BatchGroupReturning]) =
    g.map(b => testContext.BatchGroupReturning(b.string, b.column, b.prepare))
}