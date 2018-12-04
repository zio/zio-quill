package io.getquill.context.sql.norm

import io.getquill.ast._

object ExpandDistinct {

  def apply(q: Ast): Ast =
    q match {
      case Distinct(q) =>
        Distinct(apply(q))
      case q =>
        Transform(q) {
          case Aggregation(op, Distinct(q)) =>
            Aggregation(op, Distinct(apply(q)))
          case Distinct(Map(q, x, p)) =>
            Map(Distinct(Map(q, x, p)), x, p)
        }
    }
}