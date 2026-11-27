package zio.quill.context.orientdb.dsl

import zio.quill.context.orientdb.OrientDBContext

trait OrientDBDsl {
  this: OrientDBContext[_] =>

  implicit final class Like(s1: String) {
    def like(s2: String) = quote(sql"$s1 like $s2".as[Boolean])
  }
}
