package io.getquill.context.sql.dsl

import io.getquill.Query
import io.getquill.context.sql.SqlContext

@SuppressWarnings(Array("scalafix:ExplicitResultTypes"))
// noinspection TypeAnnotation
trait SqlDsl {
  this: SqlContext[_, _] =>

  implicit class Like(s1: String) {
    def like(s2: String) = quote(sql"$s1 like $s2".pure.asCondition)
  }

  implicit class ForUpdate[T](q: Query[T]) {
    def forUpdate() = quote(sql"$q FOR UPDATE".as[Query[T]])
  }
}
