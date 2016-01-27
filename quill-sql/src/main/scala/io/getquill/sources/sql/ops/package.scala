package io.getquill.sources.sql

import io.getquill._

package object ops {

  implicit class Like(s1: String) {
    def like(s2: String) = quote(infix"$s1 like $s2".as[Boolean])
  }
}
