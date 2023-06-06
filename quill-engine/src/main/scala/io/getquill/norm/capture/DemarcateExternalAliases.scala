package io.getquill.norm.capture

import io.getquill.ast._

/**
 * Walk through any Queries that a returning clause has and replace Ident of the
 * returning variable with ExternalIdent so that in later steps involving filter
 * simplification, it will not be mistakenly dealiased with a potential shadow.
 * Take this query for instance: <pre> query[TestEntity]
 * .insert(lift(TestEntity("s", 0, 1L, None))) .returningGenerated( r =>
 * (query[Dummy].filter(r => r.i == r.i).filter(d => d.i == r.i).max) ) </pre>
 * The returning clause has an alias `Ident("r")` as well as the first filter
 * clause. These two filters will be combined into one at which point the
 * meaning of `r.i` in the 2nd filter will be confused for the first filter's
 * alias (i.e. the `r` in `filter(r => ...)`. Therefore, we need to change this
 * vulnerable `r.i` in the second filter clause to an `ExternalIdent` before any
 * of the simplifications are done.
 *
 * Note that we only want to do this for Queries inside of a `Returning` clause
 * body. Other places where this needs to be done (e.g. in a Tuple that
 * `Returning` returns) are done in `ExpandReturning`.
 */
private[getquill] case class DemarcateExternalAliases(externalIdent: Ident) extends StatelessTransformer {

  def applyNonOverride(idents: Ident*)(ast: Ast) =
    if (idents.forall(_ != externalIdent)) apply(ast)
    else ast

  override def apply(ast: Ast): Ast = ast match {

    case FlatMap(q, i, b) =>
      FlatMap(apply(q), i, applyNonOverride(i)(b))

    case ConcatMap(q, i, b) =>
      ConcatMap(apply(q), i, applyNonOverride(i)(b))

    case Map(q, i, b) =>
      Map(apply(q), i, applyNonOverride(i)(b))

    case Filter(q, i, b) =>
      Filter(apply(q), i, applyNonOverride(i)(b))

    case SortBy(q, i, p, o) =>
      SortBy(apply(q), i, applyNonOverride(i)(p), o)

    case GroupBy(q, i, b) =>
      GroupBy(apply(q), i, applyNonOverride(i)(b))

    case GroupByMap(q, i, b, i1, b1) =>
      GroupByMap(apply(q), i, applyNonOverride(i)(b), i1, applyNonOverride(i1)(b1))

    case DistinctOn(q, i, b) =>
      DistinctOn(apply(q), i, applyNonOverride(i)(b))

    case Join(t, a, b, iA, iB, o) =>
      Join(t, a, b, iA, iB, applyNonOverride(iA, iB)(o))

    case FlatJoin(t, a, iA, o) =>
      FlatJoin(t, a, iA, applyNonOverride(iA)(o))

    case p @ Property.Opinionated(id @ Ident(_, quat), value, renameable, visibility) =>
      if (id.name == externalIdent.name)
        Property.Opinionated(ExternalIdent(externalIdent.name, quat), value, renameable, visibility)
      else
        p

    case other =>
      super.apply(other)
  }
}

object DemarcateExternalAliases {

  private def demarcateQueriesInBody(id: Ident, body: Ast) =
    Transform(body) {
      // Apply to the AST defined apply method about, not to the superclass method that takes Query
      case q: Query => new DemarcateExternalAliases(id).apply(q.asInstanceOf[Ast])
    }

  def apply(ast: Ast): Ast = ast match {
    case Returning(a, id, body) =>
      Returning(a, id, demarcateQueriesInBody(id, body))
    case ReturningGenerated(a, id, body) =>
      val d = demarcateQueriesInBody(id, body)
      ReturningGenerated(a, id, demarcateQueriesInBody(id, body))
    case other =>
      other
  }
}
