package zio.quill.context.sql.dsl

import zio.quill.Query
import zio.quill.context.sql.SqlContext

//noinspection TypeAnnotation
trait SqlDsl {
  this: SqlContext[_, _] =>

  implicit final class Like(s1: String) {
    def like(s2: String) = quote(sql"$s1 like $s2".pure.asCondition)
  }

  implicit final class ForUpdate[T](q: Query[T]) {
    def forUpdate() = quote(sql"$q FOR UPDATE".as[Query[T]])
  }
}
