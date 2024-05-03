package io.getquill.context

import io.getquill.ReturnAction

trait RowContext {
  type PrepareRow
  type ResultRow

  protected val identityPrepare: Prepare           = (p: PrepareRow, _: Session) => (Nil, p)
  private val _identityExtractor: Extractor[Any]   = (rr: ResultRow, _: Session) => rr
  protected def identityExtractor[T]: Extractor[T] = _identityExtractor.asInstanceOf[Extractor[T]]

  case class BatchGroup(string: String, prepare: List[Prepare])
  case class BatchGroupReturning(string: String, returningBehavior: ReturnAction, prepare: List[Prepare])

  type Prepare      = (PrepareRow, Session) => (List[Any], PrepareRow)
  type Extractor[T] = (ResultRow, Session) => T
  type Session
}
