package io.getquill.context.sql.norm

import io.getquill.ast.Constant
import io.getquill.context.sql.{ FlattenSqlQuery, SqlQuery, _ }

/**
 * In SQL Server, `Order By` clauses are only allowed in sub-queries if the sub-query has a `TOP` or `OFFSET`
 * modifier. Otherwise an exception will be thrown. This transformation adds a 'dummy' `OFFSET 0` in this
 * scenario (if an `Offset` clause does not exist already).
 */
object AddDropToNestedOrderBy {

  def applyInner(q: SqlQuery): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        q.copy(
          offset = if (q.orderBy.nonEmpty) q.offset.orElse(Some(Constant(0))) else q.offset,
          from = q.from.map(applyInner(_))
        )

      case SetOperationSqlQuery(a, op, b) => SetOperationSqlQuery(applyInner(a), op, applyInner(b))
      case UnaryOperationSqlQuery(op, a)  => UnaryOperationSqlQuery(op, applyInner(a))
    }

  private def applyInner(f: FromContext): FromContext =
    f match {
      case QueryContext(a, alias)    => QueryContext(applyInner(a), alias)
      case JoinContext(t, a, b, on)  => JoinContext(t, applyInner(a), applyInner(b), on)
      case FlatJoinContext(t, a, on) => FlatJoinContext(t, applyInner(a), on)
      case other                     => other
    }

  def apply(q: SqlQuery): SqlQuery =
    q match {
      case q: FlattenSqlQuery             => q.copy(from = q.from.map(applyInner(_)))
      case SetOperationSqlQuery(a, op, b) => SetOperationSqlQuery(applyInner(a), op, applyInner(b))
      case UnaryOperationSqlQuery(op, a)  => UnaryOperationSqlQuery(op, applyInner(a))
    }
}
