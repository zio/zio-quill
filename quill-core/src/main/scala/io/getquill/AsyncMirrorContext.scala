package io.getquill

import io.getquill.context.Context
import io.getquill.context.mirror.Row
import scala.concurrent.Future
import io.getquill.context.mirror.MirrorEncoders
import io.getquill.context.mirror.MirrorDecoders
import io.getquill.monad.ScalaFutureIOMonad
import scala.concurrent.ExecutionContext
import io.getquill.idiom.{ Idiom => BaseIdiom }
import scala.util.Try
import scala.util.Failure
import scala.util.Success

class AsyncMirrorContext[Idiom <: BaseIdiom, Naming <: NamingStrategy](val idiom: Idiom, val naming: Naming)
  extends Context[Idiom, Naming]
  with MirrorEncoders
  with MirrorDecoders
  with ScalaFutureIOMonad {

  override type PrepareRow = Row
  override type ResultRow = Row

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = QueryMirror[T]
  override type RunQuerySingleResult[T] = QueryMirror[T]
  override type RunActionResult = ActionMirror
  override type RunActionReturningResult[T] = ActionReturningMirror[T]
  override type RunBatchActionResult = BatchActionMirror
  override type RunBatchActionReturningResult[T] = BatchActionReturningMirror[T]

  override def close = ()

  def probe(statement: String): Try[_] =
    if (statement.contains("Fail"))
      Failure(new IllegalStateException("The ast contains 'Fail'"))
    else
      Success(())

  def transaction[T](f: => T) = f

  case class ActionMirror(string: String, prepareRow: PrepareRow)

  case class ActionReturningMirror[T](string: String, prepareRow: PrepareRow, extractor: Extractor[T], returningColumn: String)

  case class BatchActionMirror(groups: List[(String, List[Row])])

  case class BatchActionReturningMirror[T](groups: List[(String, String, List[PrepareRow])], extractor: Extractor[T])

  case class QueryMirror[T](string: String, prepareRow: PrepareRow, extractor: Extractor[T])

  def executeQuery[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext) =
    Future(QueryMirror(string, prepare(Row())._2, extractor))

  def executeQuerySingle[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext) =
    Future(QueryMirror(string, prepare(Row())._2, extractor))

  def executeAction(string: String, prepare: Prepare = identityPrepare)(implicit ec: ExecutionContext) =
    Future(ActionMirror(string, prepare(Row())._2))

  def executeActionReturning[O](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[O],
                                returningColumn: String)(implicit ec: ExecutionContext) =
    Future(ActionReturningMirror[O](string, prepare(Row())._2, extractor, returningColumn))

  def executeBatchAction(groups: List[BatchGroup])(implicit ec: ExecutionContext) =
    Future {
      BatchActionMirror {
        groups.map {
          case BatchGroup(string, prepare) =>
            (string, prepare.map(_(Row())._2))
        }
      }
    }

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(implicit ec: ExecutionContext) =
    Future {
      BatchActionReturningMirror[T](
        groups.map {
          case BatchGroupReturning(string, column, prepare) =>
            (string, column, prepare.map(_(Row())._2))
        }, extractor
      )
    }
}
