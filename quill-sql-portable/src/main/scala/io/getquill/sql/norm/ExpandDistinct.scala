package io.getquill.context.sql.norm

import io.getquill.ast.Visibility.Hidden
import io.getquill.ast._
import io.getquill.quat.Quat
import io.getquill.quat.QuatNestingHelper._

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
            val newIdent = Ident(x.name, valueQuat(cc.quat))
            Map(Distinct(Map(q, x, cc)), newIdent,
              Tuple(values.zipWithIndex.map {
                case (_, i) => Property(newIdent, s"_${i + 1}")
              }))

          // Situations like this:
          //    case class AdHocCaseClass(id: Int, name: String)
          //    val q = quote {
          //      query[SomeTable].map(st => AdHocCaseClass(st.id, st.name)).distinct
          //    }
          // ... need some special treatment. Otherwise their values will not be correctly expanded.
          case Distinct(Map(q, x, cc @ CaseClass(values))) =>
            val newIdent = Ident(x.name, valueQuat(cc.quat))
            Map(Distinct(Map(q, x, cc)), newIdent,
              CaseClass(values.map {
                case (name, _) => (name, Property(newIdent, name))
              }))

          // Need some special handling to address issues with distinct returning a single embedded entity i.e:
          // query[Parent].map(p => p.emb).distinct.map(e => (e.name, e.id))
          // cannot treat such a case normally or "confused" queries will result e.g:
          // SELECT p.embname, p.embid FROM (SELECT DISTINCT emb.name /* Where the heck is 'emb' coming from? */ AS embname, emb.id AS embid FROM Parent p) AS p
          case d @ Distinct(Map(q, x, p @ Property.Opinionated(_, _, _, Hidden))) => d

          // In situations where a simple, single property is being distinct-ed it is not necessary to translate it into a single-element tuple
          // for example query[Person].map(p => p.name).distinct.map(p => (p.name, pname)) can be:
          // SELECT p.name, p.name FROM (SELECT DISTINCT p.name from Person p) AS p
          // This would normally become:
          // SELECT p._1, p._1 FROM (SELECT DISTINCT p.name AS _1 from Person p) AS p
          case Distinct(Map(q, x, p @ Property(id: Ident, name))) =>
            val newQuat = valueQuat(id.quat) // force quat recomputation for perf purposes
            Map(Distinct(Map(q, x, p)), x, Property(id.copy(quat = newQuat), name))

          // Problems with distinct were first discovered in #1032. Basically, unless
          // the distinct is "expanded" adding an outer map, Ident's representing a Table will end up in invalid places
          // such as "ORDER BY tableIdent" etc...
          case Distinct(Map(q, x, p)) =>
            val newMap = Map(q, x, Tuple(List(p)))
            val newQuat = Quat.Tuple(valueQuat(p.quat)) // force quat recomputation for perf purposes
            val newIdent = Ident(x.name, newQuat)
            Map(Distinct(newMap), newIdent, Property(newIdent, "_1"))
        }
    }
}