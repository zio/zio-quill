package io.getquill.ast

sealed trait Query

case class Table(name: String) extends Query

case class Filter(query: Query, alias: Ident, body: Predicate) extends Query

case class Map(query: Query, alias: Ident, body: Expr) extends Query

case class FlatMap(query: Query, alias: Ident, body: Query) extends Query
