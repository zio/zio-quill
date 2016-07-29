package io.getquill.context.sql.dsl

import io.getquill.context.sql.SqlContext

trait SqlDsl {
  this: SqlContext[_, _] =>

  implicit class Like(s1: String) {
    def like(s2: String) = quote(infix"$s1 like $s2".as[Boolean])
  }
}
