package io.getquill

import io.getquill.context.mirror.{MirrorDecoders, MirrorEncoders, MirrorSession, Row}
import io.getquill.context.{Context, ContextVerbPrepareLambda, ContextVerbTranslate, ExecutionInfo, ProtoContext}
import io.getquill.idiom.{Idiom => BaseIdiom}
import io.getquill.monad.SyncIOMonad
import scala.language.higherKinds

import scala.util.{Failure, Success, Try}

object mirrorContextWithQueryProbing extends MirrorContext(MirrorIdiom, Literal) with QueryProbing

case class BatchActionMirrorGeneric[A](groups: List[(String, List[A])], info: ExecutionInfo)
case class BatchActionReturningMirrorGeneric[T, PrepareRow, Extractor[_]](
  groups: List[(String, ReturnAction, List[PrepareRow])],
  extractor: Extractor[T],
  info: ExecutionInfo
)

/**
 * This is supposed to emulate how Row retrieval works in JDBC Int JDBC,
 * ResultSet won't ever actually have Option values inside, so the actual
 * option-decoder needs to understand that fact e.g.
 * `Decoder[Option[Int]](java.sql.ResultSet(foo:1, etc)).getInt(1)`* and wrap it
 * into a Optional value for the equivalent row implementation:
 * `Decoder[Option[Int]](Row(foo:1, etc)).apply(1)`. (*note that
 * java.sql.ResultSet actually doesn't have this syntax because it isn't a
 * product). Similarly, when doing `ResultSet(foo:null /*Expecting an int*/,
 * etc).getInt(1)` the result will be 0 as opposed to throwing a NPE as would be
 * the scala expectation. So we need to do `Row(foo:null /*Expecting an int*/,
 * etc).apply(1)` do the same thing.
 */
class MirrorContext[+Idiom <: BaseIdiom, +Naming <: NamingStrategy](
  val idiom: Idiom,
  val naming: Naming,
  session: MirrorSession = MirrorSession("DefaultMirrorContextSession")
) extends Context[Idiom, Naming]
    with ProtoContext[Idiom, Naming]
    with ContextVerbTranslate
    with ContextVerbPrepareLambda
    with MirrorEncoders
    with MirrorDecoders
    with SyncIOMonad {

  override type PrepareRow = Row
  override type ResultRow  = Row
  override type Runner     = Unit

  override type Result[T]                        = T
  override type RunQueryResult[T]                = QueryMirror[T]
  override type RunQuerySingleResult[T]          = QueryMirror[T]
  override type RunActionResult                  = ActionMirror
  override type RunActionReturningResult[T]      = ActionReturningMirror[_, T]
  override type RunBatchActionResult             = BatchActionMirror
  override type RunBatchActionReturningResult[T] = BatchActionReturningMirror[T]
  override type Session                          = MirrorSession
  override type NullChecker                      = MirrorNullChecker
  class MirrorNullChecker extends BaseNullChecker {
    override def apply(index: Index, row: Row): Boolean = row.nullAt(index)
  }
  implicit val nullChecker: NullChecker = new MirrorNullChecker()

  override def close = ()

  def probe(statement: String): Try[_] =
    if (statement.contains("Fail"))
      Failure(new IllegalStateException("The ast contains 'Fail'"))
    else
      Success(())

  def transaction[T](f: => T) = f

  case class ActionMirror(string: String, prepareRow: PrepareRow, info: ExecutionInfo)

  case class ActionReturningMirror[T, R](
    string: String,
    prepareRow: PrepareRow,
    extractor: Extractor[T],
    returningBehavior: ReturnAction,
    info: ExecutionInfo
  )

  type BatchActionReturningMirror[T] = BatchActionReturningMirrorGeneric[T, PrepareRow, Extractor]
  val BatchActionReturningMirror = BatchActionReturningMirrorGeneric

  type BatchActionMirror = BatchActionMirrorGeneric[Row]
  val BatchActionMirror = BatchActionMirrorGeneric

  case class QueryMirror[T](string: String, prepareRow: PrepareRow, extractor: Extractor[T], info: ExecutionInfo) {
    def string(pretty: Boolean): String =
      if (pretty)
        idiom.format(string)
      else
        string
  }

  def executeQuery[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(
    info: ExecutionInfo,
    dc: Runner
  ) =
    QueryMirror(string, prepare(Row(), session)._2, extractor, info)

  def executeQuerySingle[T](
    string: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  )(info: ExecutionInfo, dc: Runner) =
    QueryMirror(string, prepare(Row(), session)._2, extractor, info)

  def executeAction(string: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner) =
    ActionMirror(string, prepare(Row(), session)._2, info)

  def executeActionReturning[O](
    string: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[O],
    returningBehavior: ReturnAction
  )(info: ExecutionInfo, dc: Runner) =
    ActionReturningMirror[O, O](string, prepare(Row(), session)._2, extractor, returningBehavior, info)

  def executeActionReturningMany[O](
    string: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[O],
    returningBehavior: ReturnAction
  )(info: ExecutionInfo, dc: Runner) =
    ActionReturningMirror[O, List[O]](string, prepare(Row(), session)._2, extractor, returningBehavior, info)

  def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner) =
    BatchActionMirror(
      groups.map { case BatchGroup(string, prepare) =>
        (string, prepare.map(_(Row(), session)._2))
      },
      info
    )

  def executeBatchActionReturning[T](
    groups: List[BatchGroupReturning],
    extractor: Extractor[T]
  )(info: ExecutionInfo, dc: Runner) =
    new BatchActionReturningMirror[T](
      groups.map { case BatchGroupReturning(string, returningBehavior, prepare) =>
        (string, returningBehavior, prepare.map(_(Row(), session)._2))
      },
      extractor,
      info
    )

  def prepareAction(string: String, prepare: Prepare = identityPrepare)(info: ExecutionInfo, dc: Runner) =
    (session: Session) => prepare(Row(), session)._2

  def prepareBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner) =
    (session: Session) =>
      groups.flatMap { case BatchGroup(string, prepare) =>
        prepare.map(_(Row(), session)._2)
      }

  override private[getquill] def prepareParams(statement: String, prepare: Prepare): Seq[String] =
    prepare(Row(), session)._2.data.map(prepareParam)
}
