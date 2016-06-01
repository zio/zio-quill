package io.getquill.sources.sql

trait Ops {
  this: SqlSource[_, _, _, _] =>

  implicit class Like(s1: String) {
    def like(s2: String) = quote(infix"$s1 like $s2".as[Boolean])
  }
}
