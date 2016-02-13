package io.getquill.sources.cassandra

import io.getquill.ast._
import io.getquill.util.Messages.fail
import io.getquill.norm.BetaReduction

case class CqlQuery(
  entity: Entity,
  filter: Option[Ast],
  orderBy: List[OrderByCriteria],
  limit: Option[Ast],
  select: List[Ast])

case class OrderByCriteria(
  property: Property,
  ordering: PropertyOrdering)

object CqlQuery {

  def apply(q: Query): CqlQuery =
    q match {
      case Map(q: Query, x, p) =>
        apply(q, select(p))
      case Aggregation(AggregationOperator.`size`, q: Query) =>
        apply(q, List(Aggregation(AggregationOperator.`size`, Constant(1))))
      case other =>
        apply(q, List())
    }

  private def apply(q: Query, select: List[Ast]): CqlQuery =
    q match {
      case Take(q: Query, limit) =>
        apply(q, Some(limit), select)
      case other =>
        apply(q, None, select)
    }

  private def apply(q: Query, limit: Option[Ast], select: List[Ast]): CqlQuery =
    q match {
      case SortBy(q: Query, x, p, o) =>
        apply(q, orderByCriterias(p, o), limit, select)
      case other =>
        apply(q, List(), limit, select)
    }

  private def apply(q: Query, orderBy: List[OrderByCriteria], limit: Option[Ast], select: List[Ast]): CqlQuery =
    q match {
      case Filter(q: Query, x, p) =>
        apply(q, Some(p), orderBy, limit, select)
      case other =>
        apply(q, None, orderBy, limit, select)
    }

  private def apply(q: Query, filter: Option[Ast], orderBy: List[OrderByCriteria], limit: Option[Ast], select: List[Ast]): CqlQuery =
    q match {
      case q: Entity =>
        new CqlQuery(q, filter, orderBy, limit, select)
      case other =>
        fail(s"Invalid cql query: $q")
    }

  private def select(ast: Ast): List[Ast] =
    ast match {
      case Tuple(values)  => values.map(select).flatten
      case p: Property    => List(p)
      case i @ Ident("?") => List(i)
      case i: Ident       => List()
      case other          => fail(s"Cql supports only properties as select elements. Found: $other")
    }

  private def orderByCriterias(ast: Ast, ordering: Ordering): List[OrderByCriteria] =
    (ast, ordering) match {
      case (Tuple(properties), ord: PropertyOrdering) => properties.map(orderByCriterias(_, ord)).flatten
      case (Tuple(properties), TupleOrdering(ord))    => properties.zip(ord).map { case (a, o) => orderByCriterias(a, o) }.flatten
      case (a: Property, o: PropertyOrdering)         => List(OrderByCriteria(a, o))
      case other                                      => fail(s"Invalid order by criteria $ast")
    }
}
