package io.getquill

import io.getquill.context.{ StandardContext, TranslateContext }
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
  extends StandardContext[Idiom, Naming]
  with TranslateContext
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

  case class ActionMirror(string: String, prepareRow: PrepareRow)(implicit val ec: ExecutionContext)

  case class ActionReturningMirror[T](string: String, prepareRow: PrepareRow, extractor: Extractor[T], returningBehavior: ReturnAction)(implicit val ec: ExecutionContext)

  case class BatchActionMirror(groups: List[(String, List[Row])])(implicit val ec: ExecutionContext)

  case class BatchActionReturningMirror[T](groups: List[(String, ReturnAction, List[PrepareRow])], extractor: Extractor[T])(implicit val ec: ExecutionContext)

  case class QueryMirror[T](string: String, prepareRow: PrepareRow, extractor: Extractor[T])(implicit val ec: ExecutionContext)

  def executeQuery[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext) =
    Future(QueryMirror(string, prepare(Row())._2, extractor))

  def executeQuerySingle[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit ec: ExecutionContext) =
    Future(QueryMirror(string, prepare(Row())._2, extractor))

  def executeAction(string: String, prepare: Prepare = identityPrepare)(implicit ec: ExecutionContext) =
    Future(ActionMirror(string, prepare(Row())._2))

  def executeActionReturning[O](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[O],
                                returningBehavior: ReturnAction)(implicit ec: ExecutionContext) =
    Future(ActionReturningMirror[O](string, prepare(Row())._2, extractor, returningBehavior))

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
          case BatchGroupReturning(string, returningBehavior, prepare) =>
            (string, returningBehavior, prepare.map(_(Row())._2))
        }, extractor
      )
    }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] =
    prepare(Row())._2.data.map(prepareParam)
}
