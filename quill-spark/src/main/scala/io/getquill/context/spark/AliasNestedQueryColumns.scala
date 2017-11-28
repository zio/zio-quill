package io.getquill.context.spark

import io.getquill.context.sql.SqlQuery
import io.getquill.context.sql.FlattenSqlQuery
import io.getquill.context.sql._
import io.getquill.ast.{ CaseClass, Ident }

object AliasNestedQueryColumns {

  def apply(q: SqlQuery): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        val aliased =
          q.select.zipWithIndex.map {
            case (s @ SelectValue(i: Ident, alias, concat), idx) => s
            case (s @ SelectValue(cc: CaseClass, alias, concat), idx) => s
            case (f, idx) => f.copy(alias = f.alias.orElse(Some(s"_${idx + 1}")))
          }

        q.copy(from = q.from.map(apply), select = aliased)

      case SetOperationSqlQuery(a, op, b) => SetOperationSqlQuery(apply(a), op, apply(b))
      case UnaryOperationSqlQuery(op, a)  => UnaryOperationSqlQuery(op, apply(a))
    }

  private def apply(f: FromContext): FromContext =
    f match {
      case QueryContext(a, alias)    => QueryContext(apply(a), alias)
      case JoinContext(t, a, b, on)  => JoinContext(t, apply(a), apply(b), on)
      case FlatJoinContext(t, a, on) => FlatJoinContext(t, apply(a), on)
      case other                     => other
    }
}