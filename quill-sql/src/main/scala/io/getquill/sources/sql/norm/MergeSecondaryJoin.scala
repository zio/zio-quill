package io.getquill.sources.sql.norm

import io.getquill.ast._

object MergeSecondaryJoin extends StatelessTransformer {

  override def apply(q: Query) = q match {
    case FlatMap(current: Join, _, body) =>
      body match {
        case FlatMap(next: Join, alias, body) if isSecondary(next) =>
          body match {
            case Map(last: Join, alias, body) if isSecondary(last) =>
              Map(merge(merge(current, next), last), alias, body)
            case _ =>
              apply(FlatMap(merge(current, next), alias, body))
          }
        case Map(last: Join, alias, body) if isSecondary(last) =>
          Map(merge(current, last), alias, body)
        case _ => q
      }
    case _ => q
  }

  private def merge(current: Join, next: Join): Join = {
    val ident = next.aliasA
    Join(next.typ, current, next.a, ident, ident, next.on)
  }

  private def isSecondary(j: Join): Boolean = j.a == j.b
}