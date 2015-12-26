package io.getquill.source.sql

import io.getquill._

package object ops {

  implicit class Like(s1: String) {
    def like(s2: String) = quote(infix"$s1 like $s2".as[Boolean])
  }

  implicit class In[T](column: T) {
    def IN(values: Seq[T]) = quote(infix"$column IN ($values)".as[Boolean])
    def in(values: Seq[T]) = IN(values)
  }

}
