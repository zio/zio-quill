package io.getquill.context.cassandra

import io.getquill.{Action, Delete, EntityQuery, Insert, Query, Update}
import io.getquill.Quoted

trait Ops {
  this: CassandraContext[_] =>

  abstract class Options[A](q: A) {
    def usingTimestamp(ts: Int): Quoted[A]  = quote(sql"$q USING TIMESTAMP $ts".as[A])
    def usingTtl(ttl: Int): Quoted[A]       = quote(sql"$q USING TTL $ttl".as[A])
    def using(ts: Int, ttl: Int): Quoted[A] = quote(sql"$q USING TIMESTAMP $ts AND TTL $ttl".as[A])
  }

  implicit class QueryOps[Q <: Query[_]](q: Q) {
    def allowFiltering: Quoted[Q] = quote(sql"$q ALLOW FILTERING".transparent.pure.as[Q])
  }

  implicit class EntityOps[A <: EntityQuery[_]](q: A) extends Options(q)

  implicit class InsertOps[A <: Insert[_]](q: A) extends Options(q) {
    def ifNotExists: Quoted[A] = quote(sql"$q IF NOT EXISTS".as[A])
  }

  implicit class UpdateOps[A <: Update[_]](q: A) extends Options(q) {
    def ifExists: Quoted[A] = quote(sql"$q IF EXISTS".as[A])
  }

  implicit class DeleteOps[A <: Delete[_]](q: A) extends Options(q) {
    def ifExists: Quoted[A] = quote(sql"$q IF EXISTS".as[A])
  }

  implicit class ActionOps[T](q: Action[T]) {
    def ifCond(cond: T => Boolean): Quoted[Action[T]] =
      quote(sql"$q IF $cond".as[Action[T]])
  }

  implicit class MapOps[K, V](map: Map[K, V]) {
    def containsValue(value: V): Quoted[Boolean] = quote(sql"$map CONTAINS $value".as[Boolean])
  }
}
