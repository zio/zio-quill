package io.getquill.source.sql

import io.getquill.ast._
import io.getquill.norm.BetaReduction
import io.getquill.util.Messages.fail

case class OrderByCriteria(property: Property, desc: Boolean)

sealed trait Source
case class TableSource(entity: Entity, alias: String) extends Source
case class QuerySource(query: SqlQuery, alias: String) extends Source
case class InfixSource(infix: Infix, alias: String) extends Source
case class OuterJoinSource(t: OuterJoinType, a: Source, b: Source, on: Ast) extends Source

sealed trait SqlQuery

sealed trait SetOperation
case object UnionOperation extends SetOperation
case object UnionAllOperation extends SetOperation

case class SetOperationSqlQuery(a: SqlQuery,
                                op: SetOperation,
                                b: SqlQuery)
    extends SqlQuery

case class SelectValue(ast: Ast, alias: Option[String] = None)

case class FlattenSqlQuery(from: List[Source],
                           where: Option[Ast] = None,
                           groupBy: List[Property] = Nil,
                           orderBy: List[OrderByCriteria] = Nil,
                           limit: Option[Ast] = None,
                           offset: Option[Ast] = None,
                           select: List[SelectValue])
    extends SqlQuery

object SqlQuery {

  def apply(query: Ast): SqlQuery =
    RenameProperties(query) match {
      case Union(a, b)    => SetOperationSqlQuery(apply(a), UnionOperation, apply(b))
      case UnionAll(a, b) => SetOperationSqlQuery(apply(a), UnionAllOperation, apply(b))
      case q: Query       => flatten(q)
      case other          => fail(s"Query not properly normalized. Please open a bug report. Ast: '$other'")
    }

  private def flatten(query: Query): FlattenSqlQuery = {
    val (sources, finalFlatMapBody) = flattenSources(query)
    flatten(sources, finalFlatMapBody, "x")
  }

  private def flattenSources(query: Query): (List[Source], Query) =
    query match {
      case FlatMap(q: Query, Ident(alias), p: Query) =>
        val source = this.source(q, alias)
        val (nestedSources, finalFlatMapBody) = flattenSources(p)
        (source +: nestedSources, finalFlatMapBody)
      case FlatMap(q: Query, Ident(alias), p: Infix) =>
        fail(s"Infix can't be use as a `flatMap` body. $query")
      case other =>
        (List.empty, other)
    }

  private def flatten(sources: List[Source], finalFlatMapBody: Ast, alias: String): FlattenSqlQuery = {

    val aliasSelect = SelectValue(Ident(alias), None) :: Nil

    def base(q: Ast, alias: String) =
      q match {
        case Map(_: GroupBy, _, _)                => FlattenSqlQuery(from = sources :+ source(q, alias), select = aliasSelect)
        case q @ (_: Map | _: Filter | _: Entity) => flatten(sources, q, alias)
        case q if (sources == Nil)                => flatten(sources, q, alias)
        case other                                => FlattenSqlQuery(from = sources :+ source(q, alias), select = aliasSelect)
      }

    finalFlatMapBody match {

      case Map(GroupBy(q, x, g), a @ Ident(alias), p) =>
        val b = base(q, alias)
        val criterias = groupByCriterias(g)
        val select = BetaReduction(p, a -> Tuple(List(g, x)))
        val flattenSelect = FlattenGroupByAggregation(x)(select)
        b.copy(groupBy = criterias, select = this.selectValues(flattenSelect))

      case GroupBy(q, Ident(alias), p) =>
        fail("A `groupBy` clause must be followed by `map`.")

      case Map(q, Ident(alias), p) =>
        base(q, alias).copy(select = selectValues(p))

      case Filter(q, Ident(alias), p) =>
        val b = base(q, alias)
        if (b.where.isEmpty)
          b.copy(where = Some(p))
        else
          FlattenSqlQuery(
            from = QuerySource(apply(q), alias) :: Nil,
            where = Some(p),
            select = aliasSelect)

      case Reverse(SortBy(q, Ident(alias), p)) =>
        val b = base(q, alias)
        val criterias = orderByCriterias(p, reverse = true)
        if (b.orderBy.isEmpty)
          b.copy(orderBy = criterias)
        else
          FlattenSqlQuery(
            from = QuerySource(apply(q), alias) :: Nil,
            orderBy = criterias,
            select = aliasSelect)

      case SortBy(q, Ident(alias), p) =>
        val b = base(q, alias)
        val criterias = orderByCriterias(p, reverse = false)
        if (b.orderBy.isEmpty)
          b.copy(orderBy = criterias)
        else
          FlattenSqlQuery(
            from = QuerySource(apply(q), alias) :: Nil,
            orderBy = criterias,
            select = aliasSelect)

      case Aggregation(op, q) =>
        val b = base(q, alias)
        b.select match {
          case head :: Nil =>
            b.copy(select = List(head.copy(ast = Aggregation(op, head.ast))))
          case other =>
            FlattenSqlQuery(
              from = QuerySource(apply(q), alias) :: Nil,
              select = List(SelectValue(Aggregation(op, Ident("*")))))
        }

      case Take(q, n) =>
        val b = base(q, alias)
        if (b.limit.isEmpty)
          b.copy(limit = Some(n))
        else
          FlattenSqlQuery(
            from = QuerySource(apply(q), alias) :: Nil,
            limit = Some(n),
            select = aliasSelect)

      case Drop(q, n) =>
        val b = base(q, alias)
        if (b.offset.isEmpty && b.limit.isEmpty)
          b.copy(offset = Some(n))
        else
          FlattenSqlQuery(
            from = QuerySource(apply(q), alias) :: Nil,
            offset = Some(n),
            select = aliasSelect)

      case other =>
        FlattenSqlQuery(from = sources :+ source(other, alias), select = aliasSelect)
    }
  }

  private def selectValues(ast: Ast) =
    ast match {
      case Tuple(values) => values.map(SelectValue(_))
      case other         => SelectValue(ast) :: Nil
    }

  private def source(ast: Ast, alias: String): Source =
    ast match {
      case entity: Entity                 => TableSource(entity, alias)
      case infix: Infix                   => InfixSource(infix, alias)
      case OuterJoin(t, a, b, ia, ib, on) => OuterJoinSource(t, source(a, ia.name), source(b, ib.name), on)
      case other                          => QuerySource(apply(other), alias)
    }

  private def groupByCriterias(ast: Ast): List[Property] =
    ast match {
      case a: Property       => List(a)
      case Tuple(properties) => properties.map(groupByCriterias).flatten
      case other             => fail(s"Invalid group by criteria $ast")
    }

  private def orderByCriterias(ast: Ast, reverse: Boolean): List[OrderByCriteria] =
    ast match {
      case a: Property       => List(OrderByCriteria(a, reverse))
      case Tuple(properties) => properties.map(orderByCriterias(_, reverse)).flatten
      case other             => fail(s"Invalid order by criteria $ast")
    }
}
