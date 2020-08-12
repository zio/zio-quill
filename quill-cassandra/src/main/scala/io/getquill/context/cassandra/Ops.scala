package io.getquill.context.cassandra

import io.getquill.{ Query, Action, Delete, Insert, EntityQuery }

trait Ops {
  this: CassandraContext[_] =>

  abstract class Options[A](q: A) {
    def usingTimestamp(ts: Int) = quote(infix"$q USING TIMESTAMP $ts".as[A])
    def usingTtl(ttl: Int) = quote(infix"$q USING TTL $ttl".as[A])
    def using(ts: Int, ttl: Int) = quote(infix"$q USING TIMESTAMP $ts AND TTL $ttl".as[A])
  }

  implicit class QueryOps[Q <: Query[_]](q: Q) {
    def allowFiltering = quote(infix"$q ALLOW FILTERING".generic.pure.as[Q])
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

  implicit class MapOps[K, V](map: Map[K, V]) {
    def containsValue(value: V) = quote(infix"$map CONTAINS $value".as[Boolean])
  }
}
