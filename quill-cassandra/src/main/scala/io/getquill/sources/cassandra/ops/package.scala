package io.getquill.sources.cassandra

import io.getquill._
import io.getquill.quotation.NonQuotedException

package object ops {

  implicit class QueryOps[Q <: Query[_]](q: Q) {
    def allowFiltering = quote(infix"$q ALLOW FILTERING".as[Q])
  }

  implicit class EntityOps[A <: EntityQuery[_]](q: A)
    extends Options(q)

  implicit class InsertOps[A <: Insert[_]](q: A)
      extends Options(q) {
    def ifNotExists = quote(infix"$q IF NOT EXISTS".as[A])
  }

  implicit class DeleteOps[A <: Delete[_]](q: A)
      extends Options(q) {
    def ifExists = quote(infix"$q IF EXISTS".as[A])
  }

  implicit class ActionOps[T](q: Action[T]) {
    def ifCond(cond: T => Boolean) =
      quote(infix"$q IF $cond".as[Action[T]])
  }
}
