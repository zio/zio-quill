package io.getquill.context.orientdb.dsl

import io.getquill.context.orientdb.OrientDBContext

trait OrientDBDsl {
  this: OrientDBContext[_] =>

  implicit class Like(s1: String) {
    def like(s2: String) = quote(infix"$s1 like $s2".as[Boolean])
  }
}