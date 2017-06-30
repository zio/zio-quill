package io.getquill.context.sql

import io.getquill.ast._
import io.getquill.context.sql.norm.FlattenGroupByAggregation
import io.getquill.norm.BetaReduction
import io.getquill.util.Messages.fail

case class OrderByCriteria(ast: Ast, ordering: PropertyOrdering)

sealed trait FromContext
case class TableContext(entity: Entity, alias: String) extends FromContext
case class QueryContext(query: SqlQuery, alias: String) extends FromContext
case class InfixContext(infix: Infix, alias: String) extends FromContext
case class JoinContext(t: JoinType, a: FromContext, b: FromContext, on: Ast) extends FromContext
case class FlatJoinContext(t: JoinType, a: FromContext, on: Ast) extends FromContext

sealed trait SqlQuery

sealed trait SetOperation
case object UnionOperation extends SetOperation
case object UnionAllOperation extends SetOperation

case class SetOperationSqlQuery(
  a:  SqlQuery,
  op: SetOperation,
  b:  SqlQuery
) extends SqlQuery

case class UnaryOperationSqlQuery(
  op: UnaryOperator,
  q:  SqlQuery
) extends SqlQuery

case class SelectValue(ast: Ast, alias: Option[String] = None)

case class FlattenSqlQuery(
  from:     List[FromContext]     = List(),
  where:    Option[Ast]           = None,
  groupBy:  List[Property]        = Nil,
  orderBy:  List[OrderByCriteria] = Nil,
  limit:    Option[Ast]           = None,
  offset:   Option[Ast]           = None,
  select:   List[SelectValue],
  distinct: Boolean               = false
)
  extends SqlQuery

object TakeDropFlatten {
  def unapply(q: Query): Option[(Query, Option[Ast], Option[Ast])] = q match {
    case Take(q: FlatMap, n) => Some((q, Some(n), None))
    case Drop(q: FlatMap, n) => Some((q, None, Some(n)))
    case _                   => None
  }
}

object SqlQuery {

  def apply(query: Ast): SqlQuery =
    query match {
      case Union(a, b)                          => SetOperationSqlQuery(apply(a), UnionOperation, apply(b))
      case UnionAll(a, b)                       => SetOperationSqlQuery(apply(a), UnionAllOperation, apply(b))
      case UnaryOperation(op, q: Query)         => UnaryOperationSqlQuery(op, apply(q))
      case _: Operation | _: Value              => FlattenSqlQuery(select = List(SelectValue(query)))
      case map @ Map(_: Nested, a, b) if a == b => flatten(map, a.name)
      case Map(q, a, b) if a == b               => apply(q)
      case TakeDropFlatten(q, limit, offset)    => flatten(q, "x").copy(limit = limit, offset = offset)
      case q: Query                             => flatten(q, "x")
      case infix: Infix                         => flatten(infix, "x")
      case other                                => fail(s"Query not properly normalized. Please open a bug report. Ast: '$other'")
    }

  private def flatten(query: Ast, alias: String): FlattenSqlQuery = {
    val (sources, finalFlatMapBody) = flattenContexts(query)
    flatten(sources, finalFlatMapBody, alias)
  }

  private def flattenContexts(query: Ast): (List[FromContext], Ast) =
    query match {
      case FlatMap(q: Query, Ident(alias), p: Query) =>
        val source = this.source(q, alias)
        val (nestedContexts, finalFlatMapBody) = flattenContexts(p)
        (source +: nestedContexts, finalFlatMapBody)
      case FlatMap(q: Query, Ident(alias), p: Infix) =>
        fail(s"Infix can't be use as a `flatMap` body. $query")
      case other =>
        (List.empty, other)
    }

  private def flatten(sources: List[FromContext], finalFlatMapBody: Ast, alias: String): FlattenSqlQuery = {

    def select(alias: String) = SelectValue(Ident(alias), None) :: Nil

    def base(q: Ast, alias: String) = {
      def nest(ctx: FromContext) = FlattenSqlQuery(from = sources :+ ctx, select = select(alias))
      q match {
        case Map(_: GroupBy, _, _)                => nest(source(q, alias))
        case Nested(q)                            => nest(QueryContext(apply(q), alias))
        case q @ (_: Map | _: Filter | _: Entity) => flatten(sources, q, alias)
        case q if (sources == Nil)                => flatten(sources, q, alias)
        case other                                => nest(source(q, alias))
      }
    }

    finalFlatMapBody match {

      case Map(GroupBy(q, x @ Ident(alias), g), a, p) =>
        val b = base(q, alias)
        val criterias = groupByCriterias(g)
        val select = BetaReduction(p, a -> Tuple(List(g, x)))
        val flattenSelect = FlattenGroupByAggregation(x)(select)
        b.copy(groupBy = criterias, select = this.selectValues(flattenSelect))

      case GroupBy(q, Ident(alias), p) =>
        fail("A `groupBy` clause must be followed by `map`.")

      case Map(q, Ident(alias), p) =>
        val b = base(q, alias)
        val agg = b.select.collect {
          case s @ SelectValue(_: Aggregation, _) => s
        }
        if (!b.distinct && agg.isEmpty)
          b.copy(select = selectValues(p))
        else
          FlattenSqlQuery(
            from = QueryContext(apply(q), alias) :: Nil,
            select = selectValues(p)
          )

      case Filter(q, Ident(alias), p) =>
        val b = base(q, alias)
        if (b.where.isEmpty)
          b.copy(where = Some(p))
        else
          FlattenSqlQuery(
            from = QueryContext(apply(q), alias) :: Nil,
            where = Some(p),
            select = select(alias)
          )

      case SortBy(q, Ident(alias), p, o) =>
        val b = base(q, alias)
        val criterias = orderByCriterias(p, o)
        if (b.orderBy.isEmpty)
          b.copy(orderBy = criterias)
        else
          FlattenSqlQuery(
            from = QueryContext(apply(q), alias) :: Nil,
            orderBy = criterias,
            select = select(alias)
          )

      case Aggregation(op, q: Query) =>
        val b = flatten(q, alias)
        b.select match {
          case head :: Nil if !b.distinct =>
            b.copy(select = List(head.copy(ast = Aggregation(op, head.ast))))
          case other =>
            FlattenSqlQuery(
              from = QueryContext(apply(q), alias) :: Nil,
              select = List(SelectValue(Aggregation(op, Ident("*"))))
            )
        }

      case Take(q, n) =>
        val b = base(q, alias)
        if (b.limit.isEmpty)
          b.copy(limit = Some(n))
        else
          FlattenSqlQuery(
            from = QueryContext(apply(q), alias) :: Nil,
            limit = Some(n),
            select = select(alias)
          )

      case Drop(q, n) =>
        val b = base(q, alias)
        if (b.offset.isEmpty && b.limit.isEmpty)
          b.copy(offset = Some(n))
        else
          FlattenSqlQuery(
            from = QueryContext(apply(q), alias) :: Nil,
            offset = Some(n),
            select = select(alias)
          )

      case Distinct(q: Query) =>
        val b = base(q, alias)
        b.copy(distinct = true)

      case other =>
        FlattenSqlQuery(from = sources :+ source(other, alias), select = select(alias))
    }
  }

  private def selectValues(ast: Ast) =
    ast match {
      case Tuple(values) => values.map(SelectValue(_))
      case other         => SelectValue(ast) :: Nil
    }

  private def source(ast: Ast, alias: String): FromContext =
    ast match {
      case entity: Entity            => TableContext(entity, alias)
      case infix: Infix              => InfixContext(infix, alias)
      case Join(t, a, b, ia, ib, on) => JoinContext(t, source(a, ia.name), source(b, ib.name), on)
      case FlatJoin(t, a, ia, on)    => FlatJoinContext(t, source(a, ia.name), on)
      case other                     => QueryContext(apply(other), alias)
    }

  private def groupByCriterias(ast: Ast): List[Property] =
    ast match {
      case a: Property       => List(a)
      case Tuple(properties) => properties.map(groupByCriterias).flatten
      case other             => fail(s"Invalid group by criteria $ast")
    }

  private def orderByCriterias(ast: Ast, ordering: Ast): List[OrderByCriteria] =
    (ast, ordering) match {
      case (Tuple(properties), ord: PropertyOrdering) => properties.map(orderByCriterias(_, ord)).flatten
      case (Tuple(properties), TupleOrdering(ord))    => properties.zip(ord).map { case (a, o) => orderByCriterias(a, o) }.flatten
      case (a, o: PropertyOrdering)                   => List(OrderByCriteria(a, o))
      case other                                      => fail(s"Invalid order by criteria $ast")
    }
}
