package io.getquill.sql.norm

import io.getquill.context.sql.{ FlatJoinContext, FlattenSqlQuery, FromContext, InfixContext, JoinContext, QueryContext, SetOperationSqlQuery, SqlQuery, TableContext, UnaryOperationSqlQuery }

trait StatelessQueryTransformer {

  def apply(q: SqlQuery): SqlQuery = {
    apply(q, true)
  }

  protected def apply(q: SqlQuery, isTopLevel: Boolean): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        expandNested(q, isTopLevel)
      case SetOperationSqlQuery(a, op, b) =>
        SetOperationSqlQuery(apply(a, false), op, apply(b, false))(q.quat)
      case UnaryOperationSqlQuery(op, q) =>
        UnaryOperationSqlQuery(op, apply(q, false))(q.quat)
    }

  protected def expandNested(q: FlattenSqlQuery, isTopLevel: Boolean): FlattenSqlQuery

  protected def expandContext(s: FromContext): FromContext =
    s match {
      case QueryContext(q, alias) =>
        QueryContext(apply(q, false), alias)
      case JoinContext(t, a, b, on) =>
        JoinContext(t, expandContext(a), expandContext(b), on)
      case FlatJoinContext(t, a, on) =>
        FlatJoinContext(t, expandContext(a), on)
      case _: TableContext | _: InfixContext => s
    }
}
