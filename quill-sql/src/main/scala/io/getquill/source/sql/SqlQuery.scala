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
import io.getquill.ast.Take

case class OrderByCriteria(property: Property, desc: Boolean)

sealed trait Source

case class TableSource(name: String, alias: String) extends Source
case class QuerySource(query: SqlQuery, alias: String) extends Source

case class SqlQuery(from: List[Source], where: Option[Ast], orderBy: List[OrderByCriteria], select: Ast)

object SqlQuery {

  def apply(query: Ast): SqlQuery =
    query match {

      // entity

      case Entity(name) =>
        SqlQuery(TableSource(name, "x") :: Nil, None, Nil, Ident("*"))

      case Map(Entity(name), Ident(alias), p) =>
        SqlQuery(TableSource(name, alias) :: Nil, None, Nil, p)

      case FlatMap(Entity(name), Ident(alias), r: Query) =>
        val nested = apply(r)
        nested.copy(from = TableSource(name, alias) :: nested.from)

      case Filter(Entity(name), Ident(alias), p) =>
        SqlQuery(TableSource(name, alias) :: Nil, Option(p), Nil, Ident("*"))

      case Reverse(SortBy(Entity(name), Ident(alias), p)) =>
        val criterias = orderByCriterias(p, reverse = true)
        SqlQuery(TableSource(name, alias) :: Nil, None, criterias, Ident("*"))

      case SortBy(Entity(name), Ident(alias), p) =>
        val criterias = orderByCriterias(p, reverse = false)
        SqlQuery(TableSource(name, alias) :: Nil, None, criterias, Ident("*"))

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

      // nested

      case FlatMap(nested(source), Ident(alias), r: Query) =>
        val nested = apply(r)
        nested.copy(from = source(alias) :: nested.from)

      case Filter(nested(source), Ident(alias), p) =>
        SqlQuery(source(alias) :: Nil, Option(p), Nil, Ident("*"))

      case other =>
        fail(s"Query is not propertly normalized, please submit a bug report. $query")
    }

  private object nested {
    def unapply(query: Query): Option[String => Source] =
      query match {
        case _: SortBy | _: Reverse | _: Take => Some(QuerySource(SqlQuery(query), _))
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
