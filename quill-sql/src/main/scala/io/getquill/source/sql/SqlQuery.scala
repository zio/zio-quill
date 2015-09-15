package io.getquill.source.sql

import io.getquill.ast.Infix
import io.getquill.ast.Ast
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.util.Messages.fail
import io.getquill.ast.Property
import io.getquill.ast.Tuple
import io.getquill.ast.Reverse
import io.getquill.ast.Take
import io.getquill.quotation.FreeVariables

case class OrderByCriteria(property: Property, desc: Boolean)

sealed trait Source {
  val alias: String
}
case class TableSource(name: String, alias: String) extends Source
case class QuerySource(query: SqlQuery, alias: String) extends Source
case class InfixSource(infix: Infix, alias: String) extends Source

case class SqlQuery(
  from: List[Source],
  where: Option[Ast] = None,
  orderBy: List[OrderByCriteria] = List(),
  limit: Option[Ast] = None,
  select: Ast = Ident("*"))

object SqlQuery {

  def apply(query: Ast): SqlQuery =
    apply(query, nest = false)

  def apply(query: Ast, nest: Boolean): SqlQuery =
    query match {

      // entity

      case Entity(name) =>
        SqlQuery(
          from = TableSource(name, "x") :: Nil)

      case Map(Entity(name), Ident(alias), p) =>
        SqlQuery(
          from = TableSource(name, alias) :: Nil,
          select = p)

      case FlatMap(Entity(name), Ident(alias), r: Query) =>
        val nested = apply(r, nest = true)
        nested.copy(from = TableSource(name, alias) :: nested.from)

      case Filter(Entity(name), Ident(alias), p) =>
        SqlQuery(
          from = TableSource(name, alias) :: Nil,
          where = Option(p))

      case Reverse(SortBy(Entity(name), Ident(alias), p)) =>
        val criterias = orderByCriterias(p, reverse = true)
        SqlQuery(
          from = TableSource(name, alias) :: Nil,
          orderBy = criterias)

      case SortBy(Entity(name), Ident(alias), p) =>
        val criterias = orderByCriterias(p, reverse = false)
        SqlQuery(
          from = TableSource(name, alias) :: Nil,
          orderBy = criterias)

      case Map(Take(Entity(name), n), Ident(x), p) =>
        SqlQuery(
          from = TableSource(name, x) :: Nil,
          limit = Some(n),
          select = p)

      // nested

      case Map(s @ nested(source), Ident(alias), p) if (nest || s.isInstanceOf[Infix]) =>
        SqlQuery(
          from = source(alias) :: Nil,
          select = p)

      case FlatMap(nested(source), Ident(alias), r: Query) =>
        val nested = apply(r)
        nested.copy(from = source(alias) :: nested.from)

      case Filter(nested(source), Ident(alias), p) =>
        SqlQuery(
          from = source(alias) :: Nil,
          where = Option(p))

      // recursion

      case Map(q: Query, x, p) =>
        apply(q).copy(select = p)

      case Reverse(SortBy(q: Query, Ident(alias), p)) =>
        val base = apply(q)
        val criterias = orderByCriterias(p, reverse = true)
        base.copy(orderBy = base.orderBy ++ criterias)

      case SortBy(q: Query, Ident(alias), p) =>
        val base = apply(q)
        val criterias = orderByCriterias(p, reverse = false)
        base.copy(orderBy = base.orderBy ++ criterias)

      case Take(q: Query, n) =>
        val base = apply(q)
        base.copy(limit = Some(n))

      case FlatMap(Entity(name), Ident(alias), r: Infix) =>
        fail(s"Infix can't be use as a `flatMap` body. $query")

      case other =>
        fail(s"Query is not propertly normalized, please submit a bug report. $query")
    }

  private object nested {
    def unapply(ast: Ast): Option[String => Source] =
      ast match {
        case _: SortBy | _: Reverse | _: Take => Some(QuerySource(SqlQuery(ast), _))
        case ast: Infix                       => Some(InfixSource(ast, _))
        case other                            => None
      }
  }

  private def orderByCriterias(ast: Ast, reverse: Boolean): List[OrderByCriteria] =
    ast match {
      case a: Property       => List(OrderByCriteria(a, reverse))
      case Tuple(properties) => properties.map(orderByCriterias(_, reverse)).flatten
      case other             => fail(s"Invalid order by criteria $ast")
    }
}
