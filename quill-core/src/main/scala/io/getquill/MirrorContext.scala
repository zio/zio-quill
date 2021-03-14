package io.getquill

import io.getquill.context.mirror.{ MirrorDecoders, MirrorEncoders, MirrorSession, Row }
import io.getquill.context.{ StandardContext, TranslateContext }
import io.getquill.idiom.{ Idiom => BaseIdiom }
import io.getquill.monad.SyncIOMonad

import scala.util.{ Failure, Success, Try }

object mirrorContextWithQueryProbing
  extends MirrorContext(MirrorIdiom, Literal) with QueryProbing

class MirrorContext[Idiom <: BaseIdiom, Naming <: NamingStrategy](val idiom: Idiom, val naming: Naming)
  extends StandardContext[Idiom, Naming]
  with TranslateContext
  with MirrorEncoders
  with MirrorDecoders
  with SyncIOMonad {

  override type PrepareRow = Row
  override type ResultRow = Row

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

  case class ActionMirror(string: String, prepareRow: PrepareRow)

  case class ActionReturningMirror[T](string: String, prepareRow: PrepareRow, extractor: Extractor[T], returningBehavior: ReturnAction)

  case class BatchActionMirror(groups: List[(String, List[Row])])

  case class BatchActionReturningMirror[T](groups: List[(String, ReturnAction, List[PrepareRow])], extractor: Extractor[T])

  case class QueryMirror[T](string: String, prepareRow: PrepareRow, extractor: Extractor[T]) {
    def string(pretty: Boolean): String =
      if (pretty)
        idiom.format(string)
      else
        string
  }

  def executeQuery[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor) =
    QueryMirror(string, prepare(Row())._2, extractor)

  def executeQuerySingle[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor) =
    QueryMirror(string, prepare(Row())._2, extractor)

  def executeAction(string: String, prepare: Prepare = identityPrepare) =
    ActionMirror(string, prepare(Row())._2)

  def executeActionReturning[O](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[O],
                                returningBehavior: ReturnAction) =
    ActionReturningMirror[O](string, prepare(Row())._2, extractor, returningBehavior)

  def executeBatchAction(groups: List[BatchGroup]) =
    BatchActionMirror {
      groups.map {
        case BatchGroup(string, prepare) =>
          (string, prepare.map(_(Row())._2))
      }
    }

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]) =
    BatchActionReturningMirror[T](
      groups.map {
        case BatchGroupReturning(string, returningBehavior, prepare) =>
          (string, returningBehavior, prepare.map(_(Row())._2))
      }, extractor
    )

  def prepareAction(string: String, prepare: Prepare = identityPrepare) =
    (session: Session) =>
      prepare(Row())._2

  def prepareBatchAction(groups: List[BatchGroup]) =
    (session: Session) =>
      groups.flatMap {
        case BatchGroup(string, prepare) =>
          prepare.map(_(Row())._2)
      }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] =
    prepare(Row())._2.data.map(prepareParam)
}
