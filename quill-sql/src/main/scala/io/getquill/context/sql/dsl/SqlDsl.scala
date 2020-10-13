package io.getquill.context.sql.dsl

import io.getquill.Query
import io.getquill.context.sql.SqlContext

trait SqlDsl {
  this: SqlContext[_, _] =>

  implicit class Like(s1: String) {
    def like(s2: String) = quote(infix"$s1 like $s2".as[Boolean])
  }

  implicit class ForUpdate[T](q: Query[T]) {
    def forUpdate() = quote(infix"$q FOR UPDATE".as[Query[T]])
  }
}
