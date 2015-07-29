package io.getquill.ast

import io.getquill.util.Show._

object QueryShow {

  import ExprShow._

  implicit val queryShow: Show[Query] = new Show[Query] {
    def show(q: Query) =
      q match {

        case Table(name) =>
          s"from[$name]"

        case Filter(source, alias, body) =>
          s"${source.show}.filter(${alias.show} => ${body.show})"

        case Map(source, alias, body) =>
          s"${source.show}.map(${alias.show} => ${body.show})"

        case FlatMap(source, alias, body) =>
          s"${source.show}.flatMap(${alias.show} => ${body.show})"
      }
  }
}