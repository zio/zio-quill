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
          case Distinct(Map(q, x, cc @ Tuple(values))) =>
            Map(Distinct(Map(q, x, cc)), x,
              Tuple(values.zipWithIndex.map {
                case (_, i) => Property(x, s"_${i + 1}")
              }))
          case Distinct(Map(q, x, cc @ CaseClass(values))) =>
            Map(Distinct(Map(q, x, cc)), x,
              CaseClass(values.map {
                case (str, _) => (str, Property(x, str))
              }))
          case Distinct(Map(q, x, p)) =>
            Map(Distinct(Map(q, x, p)), x, p)
        }
    }
}