package io.getquill.sql.norm

import io.getquill.context.sql.{
  FlatJoinContext,
  FlattenSqlQuery,
  FromContext,
  InfixContext,
  JoinContext,
  QueryContext,
  SetOperationSqlQuery,
  SqlQuery,
  TableContext,
  UnaryOperationSqlQuery
}
import io.getquill.quat.Quat

sealed trait QueryLevel {
  def isTop: Boolean
  def withoutTopQuat =
    this match {
      case QueryLevel.Top(_)       => QueryLevel.TopUnwrapped
      case QueryLevel.TopUnwrapped => QueryLevel.TopUnwrapped
      case QueryLevel.Inner        => QueryLevel.Inner
    }
}
object QueryLevel {

  /** Top-level externally-facing query */
  case class Top(topLevelQuat: Quat) extends QueryLevel { val isTop = true }

  /**
   * Top-level query that is not externally facing e.g. it's an unwrapped case
   * class AST
   */
  case object TopUnwrapped extends QueryLevel { val isTop = true }

  /** Not a Top-level query */
  case object Inner extends QueryLevel { val isTop = false }
}

trait StatelessQueryTransformer {

  def apply(q: SqlQuery, topLevelQuat: Quat = Quat.Unknown): SqlQuery =
    apply(q, QueryLevel.Top(topLevelQuat))

  protected def apply(q: SqlQuery, level: QueryLevel): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        expandNested(q, level)
      case SetOperationSqlQuery(a, op, b) =>
        SetOperationSqlQuery(apply(a, level), op, apply(b, level))(q.quat)
      case UnaryOperationSqlQuery(op, q) =>
        UnaryOperationSqlQuery(op, apply(q, level))(q.quat)
    }

  protected def expandNested(q: FlattenSqlQuery, level: QueryLevel): FlattenSqlQuery

  protected def expandContext(s: FromContext): FromContext =
    s match {
      case QueryContext(q, alias) =>
        QueryContext(apply(q, QueryLevel.Inner), alias)
      case JoinContext(t, a, b, on) =>
        JoinContext(t, expandContext(a), expandContext(b), on)
      case FlatJoinContext(t, a, on) =>
        FlatJoinContext(t, expandContext(a), on)
      case _: TableContext | _: InfixContext => s
    }
}
