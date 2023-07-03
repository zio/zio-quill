package io.getquill.context.cassandra

import io.getquill.ast._
import io.getquill.util.Messages.fail

case class CqlQuery(
  entity: Entity,
  filter: Option[Ast],
  orderBy: List[OrderByCriteria],
  limit: Option[Ast],
  select: List[Ast],
  distinct: Boolean
)

case class OrderByCriteria(
  property: Property,
  ordering: PropertyOrdering
)

object CqlQuery {

  def apply(q: Query): CqlQuery =
    q match {
      case Distinct(q: Query) =>
        apply(q, true)
      case other =>
        apply(q, false)
    }

  private def apply(q: Query, distinct: Boolean): CqlQuery =
    q match {
      case Map(q: Query, x, p) =>
        apply(q, select(p), distinct)
      case Aggregation(AggregationOperator.`size`, q: Query) =>
        apply(q, List(Aggregation(AggregationOperator.`size`, Constant.auto(1))), distinct)
      case other =>
        apply(q, List(), distinct)
    }

  private def apply(q: Query, select: List[Ast], distinct: Boolean): CqlQuery =
    q match {
      case Take(q: Query, limit) =>
        apply(q, Some(limit), select, distinct)
      case other =>
        apply(q, None, select, distinct)
    }

  private def apply(q: Query, limit: Option[Ast], select: List[Ast], distinct: Boolean): CqlQuery =
    q match {
      case SortBy(q: Query, x, p, o) =>
        apply(q, orderByCriteria(p, o), limit, select, distinct)
      case other =>
        apply(q, List(), limit, select, distinct)
    }

  private def apply(
    q: Query,
    orderBy: List[OrderByCriteria],
    limit: Option[Ast],
    select: List[Ast],
    distinct: Boolean
  ): CqlQuery =
    q match {
      case Filter(q: Query, x, p) =>
        apply(q, Some(p), orderBy, limit, select, distinct)
      case other =>
        apply(q, None, orderBy, limit, select, distinct)
    }

  private def apply(
    q: Query,
    filter: Option[Ast],
    orderBy: List[OrderByCriteria],
    limit: Option[Ast],
    select: List[Ast],
    distinct: Boolean
  ): CqlQuery =
    q match {
      case q: Entity =>
        new CqlQuery(q, filter, orderBy, limit, select, distinct)
      case (_: FlatMap) =>
        fail(s"Cql doesn't support flatMap.")
      case (_: Union) | (_: UnionAll) =>
        fail(s"Cql doesn't support union/unionAll.")
      case Join(joinType, _, _, _, _, _) =>
        fail(s"Cql doesn't support $joinType.")
      case _: GroupBy =>
        fail(s"Cql doesn't support groupBy.")
      case q =>
        fail(s"Invalid cql query: $q")
    }

  private def select(ast: Ast): List[Ast] =
    ast match {
      case Tuple(values)        => values.flatMap(select)
      case CaseClass(_, values) => values.flatMap(v => select(v._2))
      case p: Property          => List(p)
      case i: Ident             => List()
      case l: Lift              => List(l)
      case l: ScalarTag         => List(l)
      case other                => fail(s"Cql supports only properties as select elements. Found: $other")
    }

  private def orderByCriteria(ast: Ast, ordering: Ast): List[OrderByCriteria] =
    (ast, ordering) match {
      case (Tuple(properties), ord: PropertyOrdering) => properties.flatMap(orderByCriteria(_, ord))
      case (Tuple(properties), TupleOrdering(ord)) =>
        properties.zip(ord).flatMap { case (a, o) => orderByCriteria(a, o) }
      case (a: Property, o: PropertyOrdering) => List(OrderByCriteria(a, o))
      case other                              => fail(s"Invalid order by criteria $ast")
    }
}
