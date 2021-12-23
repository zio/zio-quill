package io.getquill.context.sql.dsl

import io.getquill.Query
import io.getquill.context.sql.SqlContext
import io.getquill.Quoted

trait SqlDsl {
  this: SqlContext[_, _] =>

  implicit class Like(s1: String) {
    def like(s2: String): Quoted[Boolean] = quote(infix"$s1 like $s2".as[Boolean])
  }

  implicit class ForUpdate[T](q: Query[T]) {
    def forUpdate(): Quoted[Query[T]] = quote(infix"$q FOR UPDATE".as[Query[T]])
  }
}
