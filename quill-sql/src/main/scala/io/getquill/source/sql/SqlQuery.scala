package io.getquill.source.sql

import io.getquill.ast._
import io.getquill.util.Messages.fail

case class OrderByCriteria(property: Property, desc: Boolean)

sealed trait Source {
  val alias: String
}
case class TableSource(name: String, alias: String) extends Source
case class QuerySource(query: SqlQuery, alias: String) extends Source
case class InfixSource(infix: Infix, alias: String) extends Source

sealed trait SqlQuery

sealed trait SetOperation
case object UnionOperation extends SetOperation
case object UnionAllOperation extends SetOperation

case class SetOperationSqlQuery(a: SqlQuery,
                                op: SetOperation,
                                b: SqlQuery)
    extends SqlQuery

case class FlattenSqlQuery(from: List[Source],
                           where: Option[Ast] = None,
                           groupBy: List[Property] = Nil,
                           orderBy: List[OrderByCriteria] = Nil,
                           limit: Option[Ast] = None,
                           offset: Option[Ast] = None,
                           select: Ast = Ident("*"))
    extends SqlQuery

object SqlQuery {

  def apply(query: Ast): SqlQuery =
    query match {
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
    def base(q: Ast, alias: String) =
      q match {
        case q @ (_: Map | _: Filter | _: Entity) => flatten(sources, q, alias)
        case q if (sources == Nil)                => flatten(sources, q, alias)
        case other                                => FlattenSqlQuery(from = sources :+ source(q, alias))
      }
    finalFlatMapBody match {

      case Map(q, Ident(alias), p) =>
        base(q, alias).copy(select = p)

      case Filter(q, Ident(alias), p) =>
        val b = base(q, alias)
        if (b.where.isEmpty)
          b.copy(where = Some(p))
        else
          FlattenSqlQuery(
            from = QuerySource(apply(q), alias) :: Nil,
            where = Some(p))

      case Reverse(SortBy(q, Ident(alias), p)) =>
        val b = base(q, alias)
        val criterias = orderByCriterias(p, reverse = true)
        b.copy(orderBy = b.orderBy ++ criterias)

      case SortBy(q, Ident(alias), p) =>
        val b = base(q, alias)
        val criterias = orderByCriterias(p, reverse = false)
        b.copy(orderBy = b.orderBy ++ criterias)

      case GroupBy(q, Ident(alias), p) =>
        val b = base(q, alias)
        val criterias = groupByCriterias(p)
        b.copy(groupBy = b.groupBy ++ criterias)

      case Take(q, n) =>
        val b = base(q, alias)
        if (b.limit.isEmpty)
          b.copy(limit = Some(n))
        else
          FlattenSqlQuery(
            from = QuerySource(apply(q), alias) :: Nil,
            limit = Some(n))

      case Drop(q, n) =>
        val b = base(q, alias)
        if (b.offset.isEmpty && b.limit.isEmpty)
          b.copy(offset = Some(n))
        else
          FlattenSqlQuery(
            from = QuerySource(apply(q), alias) :: Nil,
            offset = Some(n))

      case other =>
        FlattenSqlQuery(from = sources :+ source(other, alias))
    }
  }

  private def source(ast: Ast, alias: String) =
    ast match {
      case Entity(table) => TableSource(table, alias)
      case infix: Infix  => InfixSource(infix, alias)
      case other         => QuerySource(apply(other), alias)
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
