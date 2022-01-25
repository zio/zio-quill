package io.getquill

import io.getquill.context.{ ExecutionInfo, RowContext, StandardContext, TranslateContext }
import io.getquill.context.mirror.{ MirrorDecoders, MirrorEncoders, MirrorSession, Row }

import scala.concurrent.Future
import io.getquill.monad.ScalaFutureIOMonad

import scala.concurrent.ExecutionContext
import io.getquill.idiom.{ Idiom => BaseIdiom }

import scala.util.Try
import scala.util.Failure
import scala.util.Success

class AsyncMirrorContext[Idiom <: BaseIdiom, Naming <: NamingStrategy](val idiom: Idiom, val naming: Naming, session: MirrorSession = MirrorSession("DefaultMirrorContextSession"))
  extends StandardContext[Idiom, Naming]
  with RowContext
  with TranslateContext
  with MirrorEncoders
  with MirrorDecoders
  with ScalaFutureIOMonad {

  override type PrepareRow = Row
  override type ResultRow = Row
  override type Session = MirrorSession

  override type Result[T] = Future[T]
  override type RunQueryResult[T] = QueryMirror[T]
  override type RunQuerySingleResult[T] = QueryMirror[T]
  override type RunActionResult = ActionMirror
  override type RunActionReturningResult[T] = ActionReturningMirror[T]
  override type RunBatchActionResult = BatchActionMirror
  override type RunBatchActionReturningResult[T] = BatchActionReturningMirror[T]
  override type Runner = Unit

  override def close = ()

  def probe(statement: String): Try[_] =
    if (statement.contains("Fail"))
      Failure(new IllegalStateException("The ast contains 'Fail'"))
    else
      Success(())

  def transaction[T](f: => T) = f

  case class TransactionalExecutionContext(ec: ExecutionContext) extends ExecutionContext {
    def execute(runnable: Runnable): Unit =
      ec.execute(runnable)

    def reportFailure(cause: Throwable): Unit =
      ec.reportFailure(cause)
  }

  override def performIO[T](io: IO[T, _], transactional: Boolean = false)(implicit ec: ExecutionContext): Result[T] =
    transactional match {
      case true  => super.performIO(io, transactional)(TransactionalExecutionContext(ec))
      case false => super.performIO(io, transactional)
    }

  case class ActionMirror(string: String, prepareRow: PrepareRow, info: ExecutionInfo)(implicit val ec: ExecutionContext)

  case class ActionReturningMirror[T](string: String, prepareRow: PrepareRow, extractor: Extractor[T], returningBehavior: ReturnAction, info: ExecutionInfo)(implicit val ec: ExecutionContext)

  case class BatchActionMirror(groups: List[(String, List[Row])], info: ExecutionInfo)(implicit val ec: ExecutionContext)

  case class BatchActionReturningMirror[T](groups: List[(String, ReturnAction, List[PrepareRow])], extractor: Extractor[T], info: ExecutionInfo)(implicit val ec: ExecutionContext)

  case class QueryMirror[T](string: String, prepareRow: PrepareRow, extractor: Extractor[T], info: ExecutionInfo)(implicit val ec: ExecutionContext)

  def executeQuery[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(executionInfo: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext) =
    Future(QueryMirror(string, prepare(Row(), session)._2, extractor, executionInfo))

  def executeQuerySingle[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(executionInfo: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext) =
    Future(QueryMirror(string, prepare(Row(), session)._2, extractor, executionInfo))

  def executeAction(string: String, prepare: Prepare = identityPrepare)(executionInfo: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext) =
    Future(ActionMirror(string, prepare(Row(), session)._2, executionInfo))

  def executeActionReturning[O](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction)(executionInfo: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext) =
    Future(ActionReturningMirror[O](string, prepare(Row(), session)._2, extractor, returningBehavior, executionInfo))

  def executeBatchAction(groups: List[BatchGroup])(executionInfo: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext) =
    Future {
      BatchActionMirror(
        groups.map {
          case BatchGroup(string, prepare) =>
            (string, prepare.map(_(Row(), session)._2))
        },
        executionInfo
      )
    }

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(executionInfo: ExecutionInfo, dc: Runner)(implicit ec: ExecutionContext) =
    Future {
      BatchActionReturningMirror[T](
        groups.map {
          case BatchGroupReturning(string, returningBehavior, prepare) =>
            (string, returningBehavior, prepare.map(_(Row(), session)._2))
        }, extractor, executionInfo
      )
    }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] =
    prepare(Row(), session)._2.data.map(prepareParam)
}
