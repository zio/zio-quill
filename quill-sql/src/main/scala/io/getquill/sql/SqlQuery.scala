package io.getquill.sql

import io.getquill.ast._

case class Source(table: String, alias: String)
case class SqlQuery(from: List[Source], where: Option[Predicate], select: Expr)

object SqlQuery {

  def apply(query: Query) =
    flatten(query) match {
      case (from, where, select) =>
        new SqlQuery(from, where, select)
    }

  private def flatten(query: Query): (List[Source], Option[Predicate], Expr) = {
    query match {
      case FlatMap(Table(name), Ident(alias), r) =>
        val (sources, predicate, expr) = flatten(r)
        (Source(name, alias) :: sources, predicate, expr)
      case Filter(Table(name), Ident(alias), p) =>
        (Source(name, alias) :: Nil, Option(p), Ident(alias))
      case Map(Table(name), Ident(alias), p) =>
        (List(Source(name, alias)), None, p)
      case Map(q, x, p) =>
        val (sources, predicate, expr) = flatten(q)
        (sources, predicate, p)
      case other =>
        import io.getquill.util.Show._
        import io.getquill.ast.QueryShow._
        throw new IllegalStateException(s"Query is not propertly normalized, please submit a bug report. ${query.show}")
    }
  }
}