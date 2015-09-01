package io.getquill.source.sql

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

case class OrderByCriteria(property: Property, desc: Boolean)
case class Source(table: String, alias: String)
case class SqlQuery(from: List[Source], where: Option[Ast], orderBy: List[OrderByCriteria], select: Ast)

object SqlQuery {

  def apply(query: Ast): SqlQuery =
    query match {

      case Map(Entity(name), Ident(alias), p) =>
        SqlQuery(List(Source(name, alias)), None, List(), p)

      case FlatMap(Entity(name), Ident(alias), r: Query) =>
        val nested = apply(r)
        SqlQuery(Source(name, alias) :: nested.from, nested.where, nested.orderBy, nested.select)

      case Filter(Entity(name), Ident(alias), p) =>
        SqlQuery(Source(name, alias) :: Nil, Option(p), List(), Ident(alias))

      case Reverse(SortBy(Entity(name), Ident(alias), p)) =>
        val criterias = orderByCriterias(p, reverse = true)
        SqlQuery(List(Source(name, alias)), None, criterias, Ident(alias))

      case SortBy(Entity(name), Ident(alias), p) =>
        val criterias = orderByCriterias(p, reverse = false)
        SqlQuery(List(Source(name, alias)), None, criterias, Ident(alias))

      case Map(q: Query, x, p) =>
        val base = apply(q)
        SqlQuery(base.from, base.where, base.orderBy, p)

      case Reverse(SortBy(q: Query, Ident(alias), p)) =>
        val base = apply(q)
        val criterias = orderByCriterias(p, reverse = true)
        SqlQuery(base.from, base.where, base.orderBy ++ criterias, Ident(alias))

      case SortBy(q: Query, Ident(alias), p) =>
        val base = apply(q)
        val criterias = orderByCriterias(p, reverse = false)
        SqlQuery(base.from, base.where, base.orderBy ++ criterias, Ident(alias))

      case other =>
        fail(s"Query is not propertly normalized, please submit a bug report. $query")
    }

  private def orderByCriterias(ast: Ast, reverse: Boolean): List[OrderByCriteria] =
    ast match {
      case a: Property       => List(OrderByCriteria(a, reverse))
      case Tuple(properties) => properties.map(orderByCriterias(_, reverse)).flatten
      case other             => fail(s"Invalid order by criteria $ast")
    }
}
