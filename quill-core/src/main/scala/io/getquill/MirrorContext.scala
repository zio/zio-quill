package io.getquill

import io.getquill.context.Context
import io.getquill.context.mirror.{ MirrorDecoders, MirrorEncoders, Row }
import io.getquill.idiom.{ Idiom => BaseIdiom }
import scala.util.{ Failure, Success, Try }
import io.getquill.monad.SyncIOMonad

class MirrorContextWithQueryProbing[Idiom <: BaseIdiom, Naming <: NamingStrategy]
  extends MirrorContext[Idiom, Naming] with QueryProbing

class MirrorContext[Idiom <: BaseIdiom, Naming <: NamingStrategy]
  extends Context[Idiom, Naming]
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

  override def close = ()

  def probe(statement: String): Try[_] =
    if (statement.contains("Fail"))
      Failure(new IllegalStateException("The ast contains 'Fail'"))
    else
      Success(())

  def transaction[T](f: => T) = f

  case class ActionMirror(string: String, prepareRow: PrepareRow)

  case class ActionReturningMirror[T](string: String, prepareRow: Row, extractor: Row => T, returningColumn: String)

  case class BatchActionMirror(groups: List[(String, List[Row])])

  case class BatchActionReturningMirror[T](groups: List[(String, String, List[Row])], extractor: Row => T)

  case class QueryMirror[T](string: String, prepareRow: Row, extractor: Row => T)

  def executeQuery[T](string: String, prepare: Row => Row = identity, extractor: Row => T = identity[Row] _) =
    QueryMirror(string, prepare(Row()), extractor)

  def executeQuerySingle[T](string: String, prepare: Row => Row = identity, extractor: Row => T = identity[Row] _) =
    QueryMirror(string, prepare(Row()), extractor)

  def executeAction(string: String, prepare: Row => Row = identity) =
    ActionMirror(string, prepare(Row()))

  def executeActionReturning[O](string: String, prepare: Row => Row = identity, extractor: Row => O,
                                returningColumn: String) =
    ActionReturningMirror[O](string, prepare(Row()), extractor, returningColumn)

  def executeBatchAction(groups: List[BatchGroup]) =
    BatchActionMirror {
      groups.map {
        case BatchGroup(string, prepare) =>
          (string, prepare.map(_(Row())))
      }
    }

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Row => T) =
    BatchActionReturningMirror[T](
      groups.map {
        case BatchGroupReturning(string, column, prepare) =>
          (string, column, prepare.map(_(Row())))
      }, extractor
    )
}
