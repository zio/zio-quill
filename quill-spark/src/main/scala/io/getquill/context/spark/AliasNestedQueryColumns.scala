package io.getquill.context.spark

import io.getquill.context.sql.SqlQuery
import io.getquill.context.sql.FlattenSqlQuery
import io.getquill.context.sql._
import io.getquill.quat.Quat

object AliasNestedQueryColumns {

  object ZipMatch {
    def unapply[A, B](seqs: (Seq[A], Seq[B])) =
      if (seqs._1.length == seqs._2.length) Some(seqs._1.zip(seqs._2))
      else None
  }

  def apply(q: SqlQuery): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        val newSelects =
          q.quat match {
            case Quat.Product(fields) =>
              val duo = (fields.map(_._1), q.select)
              (duo: @unchecked) match {
                case ZipMatch(fieldsAndSelects) =>
                  fieldsAndSelects.map { case (field, select) => select.copy(alias = Some(field)) }
                case _ =>
                  q.select
              }
            case _ =>
              q.select
          }

        q.copy(from = q.from.map(apply), select = newSelects.toList)(q.quat)

      case SetOperationSqlQuery(a, op, b) => SetOperationSqlQuery(apply(a), op, apply(b))(q.quat)
      case UnaryOperationSqlQuery(op, a)  => UnaryOperationSqlQuery(op, apply(a))(q.quat)
    }

  private def apply(f: FromContext): FromContext =
    f match {
      case QueryContext(a, alias)    => QueryContext(apply(a), alias)
      case JoinContext(t, a, b, on)  => JoinContext(t, apply(a), apply(b), on)
      case FlatJoinContext(t, a, on) => FlatJoinContext(t, apply(a), on)
      case other                     => other
    }
}