package io.getquill.context

import io.getquill.ReturnAction

trait RowContext {
  type PrepareRow
  type ResultRow

  protected val identityPrepare: Prepare = (p: PrepareRow, s: Session) => (Nil, p)
  protected val identityExtractor = (rr: ResultRow, s: Session) => rr

  case class BatchGroup(string: String, prepare: List[Prepare])
  case class BatchGroupReturning(string: String, returningBehavior: ReturnAction, prepare: List[Prepare])

  type Prepare = (PrepareRow, Session) => (List[Any], PrepareRow)
  type Extractor[T] = (ResultRow, Session) => T
  type Session
}
