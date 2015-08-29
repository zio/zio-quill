package io.getquill.source.sql

import io.getquill.ast.Ast
import io.getquill.ast.AstShow.queryShow
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.util.Show.Shower
import io.getquill.util.Messages._
import io.getquill.ast.SortBy
import io.getquill.ast.SortBy
import io.getquill.norm.BetaReduction

case class Source(table: String, alias: String)
case class SqlQuery(from: List[Source], where: Option[Ast], sortBy: Option[Ast], select: Ast)

object SqlQuery {

  def apply(query: Ast): SqlQuery =
    query match {

      case FlatMap(Entity(name), Ident(alias), r: Query) =>
        val nested = apply(r)
        SqlQuery(Source(name, alias) :: nested.from, nested.where, nested.sortBy, nested.select)

      case Filter(Entity(name), Ident(alias), p) =>
        SqlQuery(Source(name, alias) :: Nil, Option(p), None, Ident(alias))

      case Map(Entity(name), Ident(alias), p) =>
        SqlQuery(List(Source(name, alias)), None, None, p)

      case SortBy(Entity(name), Ident(alias), p) =>
        SqlQuery(List(Source(name, alias)), None, Some(p), Ident(alias))

      case Map(q: Query, x, p) =>
        val base = apply(q)
        SqlQuery(base.from, base.where, base.sortBy, p)

      case SortBy(q, x, p) =>
        val base = apply(q)
        SqlQuery(base.from, base.where, Some(p), base.select)

      case other =>
        fail(s"Query is not propertly normalized, please submit a bug report. $query")
    }
}
