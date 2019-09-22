package io.getquill.context.sql.norm

import io.getquill.ast.Visibility.Hidden
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

          // Situations like this:
          //    case class AdHocCaseClass(id: Int, name: String)
          //    val q = quote {
          //      query[SomeTable].map(st => AdHocCaseClass(st.id, st.name)).distinct
          //    }
          // ... need some special treatment. Otherwise their values will not be correctly expanded.
          case Distinct(Map(q, x, cc @ CaseClass(values))) =>
            Map(Distinct(Map(q, x, cc)), x,
              CaseClass(values.map {
                case (name, _) => (name, Property(x, name))
              }))

          // Need some special handling to address issues with distinct returning a single embedded entity i.e:
          // query[Parent].map(p => p.emb).distinct.map(e => (e.name, e.id))
          // cannot treat such a case normally or "confused" queries will result e.g:
          // SELECT p.embname, p.embid FROM (SELECT DISTINCT emb.name /* Where the heck is 'emb' coming from? */ AS embname, emb.id AS embid FROM Parent p) AS p
          case d @ Distinct(Map(q, x, p @ Property.Opinionated(_, _, _, Hidden))) => d

          // Problems with distinct were first discovered in #1032. Basically, unless
          // the distinct is "expanded" adding an outer map, Ident's representing a Table will end up in invalid places
          // such as "ORDER BY tableIdent" etc...
          case Distinct(Map(q, x, p)) =>
            Map(Distinct(Map(q, x, p)), x, p)
        }
    }
}