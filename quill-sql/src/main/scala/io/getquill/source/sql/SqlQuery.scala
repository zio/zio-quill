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

case class Source(table: String, alias: String)
case class SqlQuery(from: List[Source], where: Option[Ast], select: Ast)

object SqlQuery {

  def apply(ast: Ast) =
    ast match {
      case query: Query =>
        flatten(query) match {
          case (from, where, select) =>
            new SqlQuery(from, where, select)
        }
      case other =>
        throw new IllegalStateException(s"Query is not propertly normalized, please submit a bug report. $other")
    }

  private def flatten(query: Query): (List[Source], Option[Ast], Ast) = {
    query match {
      case FlatMap(Entity(name), Ident(alias), r: Query) =>
        val (sources, predicate, ast) = flatten(r)
        (Source(name, alias) :: sources, predicate, ast)
      case Filter(Entity(name), Ident(alias), p) =>
        (Source(name, alias) :: Nil, Option(p), Ident(alias))
      case Map(Entity(name), Ident(alias), p) =>
        (List(Source(name, alias)), None, p)
      case Map(q: Query, x, p) =>
        val (sources, predicate, ast) = flatten(q)
        (sources, predicate, p)
      case other =>
        import io.getquill.util.Show._
        import io.getquill.ast.AstShow._
        throw new IllegalStateException(s"Query is not propertly normalized, please submit a bug report. ${query.show}")
    }
  }
}
