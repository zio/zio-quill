package io.getquill

import io.getquill.context.mirror.{ MirrorDecoders, MirrorEncoders, MirrorSession, Row }
import io.getquill.context.{ ExecutionInfo, ProtoContext, StandardContext, TranslateContext }
import io.getquill.idiom.{ Idiom => BaseIdiom }
import io.getquill.monad.SyncIOMonad

import scala.util.{ Failure, Success, Try }

object mirrorContextWithQueryProbing
  extends MirrorContext(MirrorIdiom, Literal) with QueryProbing

class MirrorContext[Idiom <: BaseIdiom, Naming <: NamingStrategy](val idiom: Idiom, val naming: Naming, session: MirrorSession = MirrorSession("DefaultMirrorContextSession"))
  extends StandardContext[Idiom, Naming]
  with ProtoContext[Idiom, Naming]
  with TranslateContext
  with MirrorEncoders
  with MirrorDecoders
  with SyncIOMonad {

  override type PrepareRow = Row
  override type ResultRow = Row
  override type Runner = Unit

  override type Result[T] = T
  override type RunQueryResult[T] = QueryMirror[T]
  override type RunQuerySingleResult[T] = QueryMirror[T]
  override type RunActionResult = ActionMirror
  override type RunActionReturningResult[T] = ActionReturningMirror[T]
  override type RunBatchActionResult = BatchActionMirror
  override type RunBatchActionReturningResult[T] = BatchActionReturningMirror[T]
  override type Session = MirrorSession

  override def close = ()

  def probe(statement: String): Try[_] =
    if (statement.contains("Fail"))
      Failure(new IllegalStateException("The ast contains 'Fail'"))
    else
      Success(())

  def transaction[T](f: => T) = f

  case class ActionMirror(string: String, prepareRow: PrepareRow, info: ExecutionInfo)

  case class ActionReturningMirror[T](string: String, prepareRow: PrepareRow, extractor: Extractor[T], returningBehavior: ReturnAction, info: ExecutionInfo)

  case class BatchActionMirror(groups: List[(String, List[Row])], info: ExecutionInfo)

  case class BatchActionReturningMirror[T](groups: List[(String, ReturnAction, List[PrepareRow])], extractor: Extractor[T], info: ExecutionInfo)

  case class QueryMirror[T](string: String, prepareRow: PrepareRow, extractor: Extractor[T], info: ExecutionInfo) {
    def string(pretty: Boolean): String =
      if (pretty)
        idiom.format(string)
      else
        string
  }

  def executeQuery[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner) =
    QueryMirror(string, prepare(Row(), session)._2, extractor, info)

  def executeQuerySingle[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(info: ExecutionInfo, dc: Runner) =
    QueryMirror(string, prepare(Row(), session)._2, extractor, info)

  def executeAction(string: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner) =
    ActionMirror(string, prepare(Row(), session)._2, info)

  def executeActionReturning[O](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningBehavior: ReturnAction)(info: ExecutionInfo, dc: Runner) =
    ActionReturningMirror[O](string, prepare(Row(), session)._2, extractor, returningBehavior, info)

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner) =
    BatchActionMirror(
      groups.map {
        case BatchGroup(string, prepare) =>
          (string, prepare.map(_(Row(), session)._2))
      },
      info
    )

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T])(info: ExecutionInfo, dc: Runner) =
    BatchActionReturningMirror[T](
      groups.map {
        case BatchGroupReturning(string, returningBehavior, prepare) =>
          (string, returningBehavior, prepare.map(_(Row(), session)._2))
      }, extractor, info
    )

  def prepareAction(string: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner) =
    (session: Session) =>
      prepare(Row(), session)._2

  def prepareBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner) =
    (session: Session) =>
      groups.flatMap {
        case BatchGroup(string, prepare) =>
          prepare.map(_(Row(), session)._2)
      }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] =
    prepare(Row(), session)._2.data.map(prepareParam)
}
